#import "CAPLlama.h"
#import "LlamaContext.h"

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNLlamaSpec.h"
#endif

@implementation CAPLlama

static NSMutableDictionary *llamaContexts;
static double llamaContextLimit = -1;
static dispatch_queue_t llamaDQueue;

+ (void)toggleNativeLog:(BOOL)enabled {
    void (^onEmitLog)(NSString *level, NSString *text) = nil;
    if (enabled) {
        onEmitLog = ^(NSString *level, NSString *text) {
            // Replace with appropriate event handling logic
            NSLog(@"Native Log - Level: %@, Text: %@", level, text);
        };
    }
    [LlamaContext toggleNativeLog:enabled onEmitLog:onEmitLog];
}

+ (void)setContextLimit:(double)limit {
    llamaContextLimit = limit;
}

+ (NSDictionary *)modelInfo:(NSString *)path withSkip:(NSArray *)skip {
    return [LlamaContext modelInfo:path skip:skip];
}

+ (NSDictionary *)initContext:(double)contextId withContextParams:(NSDictionary *)contextParams {
    NSNumber *contextIdNumber = [NSNumber numberWithDouble:contextId];
    if (llamaContexts[contextIdNumber] != nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context already exists" userInfo:nil];
    }

    if (llamaDQueue == nil) {
        llamaDQueue = dispatch_queue_create("com.rnllama", DISPATCH_QUEUE_SERIAL);
    }

    if (llamaContexts == nil) {
        llamaContexts = [[NSMutableDictionary alloc] init];
    }

    if (llamaContextLimit > -1 && [llamaContexts count] >= llamaContextLimit) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context limit reached" userInfo:nil];
    }

    LlamaContext *context = [LlamaContext initWithParams:contextParams onProgress:^(unsigned int progress) {
        dispatch_async(dispatch_get_main_queue(), ^{
            // Replace with appropriate event handling logic
            NSLog(@"Init Context Progress - Context ID: %f, Progress: %u", contextId, progress);
        });
    }];
    if (![context isModelLoaded]) {
        @throw [NSException exceptionWithName:@"llama_cpp_error" reason:@"Failed to load the model" userInfo:nil];
    }

    [llamaContexts setObject:context forKey:contextIdNumber];

    return @{
        @"gpu": @([context isMetalEnabled]),
        @"reasonNoGPU": [context reasonNoMetal],
        @"model": [context modelInfo],
    };
}

+ (NSDictionary *)getFormattedChat:(double)contextId withMessages:(NSString *)messages withTemplate:(NSString *)chatTemplate withParams:(NSDictionary *)params {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([params[@"jinja"] boolValue]) {
        NSString *jsonSchema = params[@"json_schema"];
        NSString *tools = params[@"tools"];
        bool parallelToolCalls = [params[@"parallel_tool_calls"] boolValue];
        NSString *toolChoice = params[@"tool_choice"];
        return [context getFormattedChatWithJinja:messages withChatTemplate:chatTemplate withJsonSchema:jsonSchema withTools:tools withParallelToolCalls:parallelToolCalls withToolChoice:toolChoice];
    } else {
        NSString *chat = [context getFormattedChat:messages withChatTemplate:chatTemplate];
        return @{@"prompt": chat};
    }
}

+ (NSDictionary *)loadSession:(double)contextId withFilePath:(NSString *)filePath {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([context isPredicting]) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context is busy" userInfo:nil];
    }
    __block NSDictionary *result;
    dispatch_sync(llamaDQueue, ^{
        @try {
            @autoreleasepool {
                result = [context loadSession:filePath];
            }
        } @catch (NSException *exception) {
            @throw exception;
        }
    });
    return result;
}

+ (int)saveSession:(double)contextId withFilePath:(NSString *)filePath withSize:(double)size {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([context isPredicting]) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context is busy" userInfo:nil];
    }
    __block int count;
    dispatch_sync(llamaDQueue, ^{
        @try {
            @autoreleasepool {
                count = [context saveSession:filePath size:(int)size];
            }
        } @catch (NSException *exception) {
            @throw exception;
        }
    });
    return count;
}

+ (NSDictionary *)completion:(double)contextId withCompletionParams:(NSDictionary *)completionParams {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([context isPredicting]) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context is busy" userInfo:nil];
    }
    __block NSDictionary *completionResult;
    dispatch_sync(llamaDQueue, ^{
        @try {
            @autoreleasepool {
                completionResult = [context completion:completionParams
                    onToken:^(NSMutableDictionary *tokenResult) {
                        if (![completionParams[@"emit_partial_completion"] boolValue]) return;
                        dispatch_async(dispatch_get_main_queue(), ^{
                            // Replace with appropriate event handling logic
                            NSLog(@"Token - Context ID: %f, Token Result: %@", contextId, tokenResult);
                            [tokenResult release];
                        });
                    }
                ];
            }
        } @catch (NSException *exception) {
            @throw exception;
            [context stopCompletion];
        }
    });
    return completionResult;
}

+ (void)stopCompletion:(double)contextId {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    [context stopCompletion];
}

+ (NSDictionary *)tokenize:(double)contextId text:(NSString *)text {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    NSMutableArray *tokens = [context tokenize:text];
    NSDictionary *result = @{ @"tokens": tokens };
    [tokens release];
    return result;
}

+ (NSDictionary *)detokenize:(double)contextId tokens:(NSArray *)tokens {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    NSDictionary *result = @{ @"text": [context detokenize:tokens] };
    return result;
}

+ (NSDictionary *)embedding:(double)contextId text:(NSString *)text params:(NSDictionary *)params {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    @try {
        NSDictionary *embedding = [context embedding:text params:params];
        return embedding;
    } @catch (NSException *exception) {
        @throw exception;
    }
}

+ (NSString *)bench:(double)contextId pp:(int)pp tg:(int)tg pl:(int)pl nr:(int)nr {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    @try {
        NSString *benchResults = [context bench:pp tg:tg pl:pl nr:nr];
        return benchResults;
    } @catch (NSException *exception) {
        @throw exception;
    }
}

+ (void)applyLoraAdapters:(double)contextId withLoraAdapters:(NSArray *)loraAdapters {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([context isPredicting]) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context is busy" userInfo:nil];
    }
    [context applyLoraAdapters:loraAdapters];
}

+ (void)removeLoraAdapters:(double)contextId {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if ([context isPredicting]) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context is busy" userInfo:nil];
    }
    [context removeLoraAdapters];
}

+ (NSArray *)getLoadedLoraAdapters:(double)contextId {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    return [context getLoadedLoraAdapters];
}

+ (void)releaseContext:(double)contextId {
    LlamaContext *context = llamaContexts[[NSNumber numberWithDouble:contextId]];
    if (context == nil) {
        @throw [NSException exceptionWithName:@"llama_error" reason:@"Context not found" userInfo:nil];
    }
    if (![context isModelLoaded]) {
      [context interruptLoad];
    }
    [context stopCompletion];
    dispatch_barrier_sync(llamaDQueue, ^{});
    [context invalidate];
    [llamaContexts removeObjectForKey:[NSNumber numberWithDouble:contextId]];
}

+ (void)releaseAllContexts {
    if (llamaContexts == nil) {
        return;
    }

    for (NSNumber *contextId in llamaContexts) {
        LlamaContext *context = llamaContexts[contextId];
        [context stopCompletion];
        dispatch_barrier_sync(llamaDQueue, ^{});
        [context invalidate];
    }

    [llamaContexts removeAllObjects];
    [llamaContexts release];
    llamaContexts = nil;

    if (llamaDQueue != nil) {
        dispatch_release(llamaDQueue);
        llamaDQueue = nil;
    }
}

// Don't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeRNLlamaSpecJSI>(params);
}
#endif

@end
