#ifdef __cplusplus
#include <list>
#if RNLLAMA_BUILD_FROM_SOURCE
#import "llama.h"
#import "llama-impl.h"
#import "ggml.h"
#import "rn-llama.h"
#import "rn-completion.h"
#import "rn-slot.h"
#import "rn-slot-manager.h"
#import "json-schema-to-grammar.h"
#import "common.h"
#else
#import <capllama/llama.h>
#import <capllama/llama-impl.h>
#import <capllama/ggml.h>
#import <capllama/rn-llama.h>
#import <capllama/rn-completion.h>
#import <capllama/rn-slot.h>
#import <capllama/rn-slot-manager.h>
#import <capllama/json-schema-to-grammar.h>
#import <capllama/common.h>
#endif
#endif


@interface LlamaContext : NSObject {
    bool is_metal_enabled;
    bool is_model_loaded;
    NSString * reason_no_metal;
    NSArray * used_devices;
    NSString * system_info;

    void (^onProgress)(unsigned int progress);

    rnllama::llama_rn_context * llama;
}

+ (void)toggleNativeLog:(BOOL)enabled onEmitLog:(void (^)(NSString *level, NSString *text))onEmitLog;
+ (NSDictionary *)modelInfo:(NSString *)path skip:(NSArray *)skip;
+ (instancetype)initWithParams:(NSDictionary *)params onProgress:(void (^)(unsigned int progress))onProgress;
+ (NSString *)getBackendDevicesInfo;
- (NSArray *)usedDevices;
- (void)interruptLoad;
- (bool)isMetalEnabled;
- (NSString *)reasonNoMetal;
- (NSDictionary *)modelInfo;
- (NSString *)systemInfo;
- (bool)isModelLoaded;
- (bool)isPredicting;
- (NSDictionary *)completion:(NSDictionary *)params onToken:(void (^)(NSMutableDictionary *tokenResult))onToken;
- (void)stopCompletion;
- (NSNumber *)queueCompletion:(NSDictionary *)params onToken:(void (^)(NSMutableDictionary *tokenResult))onToken onComplete:(void (^)(NSDictionary *result))onComplete;
- (NSNumber *)queueEmbedding:(NSString *)text params:(NSDictionary *)params onResult:(void (^)(int32_t requestId, NSArray *embedding))onResult;
- (NSNumber *)queueRerank:(NSString *)query documents:(NSArray<NSString *> *)documents params:(NSDictionary *)params onResults:(void (^)(int32_t requestId, NSArray *results))onResults;
- (void)cancelRequest:(NSNumber *)requestId;
- (BOOL)enableParallelMode:(int)nParallel nBatch:(int)nBatch;
- (void)disableParallelMode;
- (NSArray *)tokenize:(NSString *)text;
- (NSString *)detokenize:(NSArray *)tokens;
- (NSArray *)getVocab;
- (NSDictionary *)embedding:(NSString *)text params:(NSDictionary *)params;
- (NSDictionary *)getFormattedChatWithJinja:(NSString *)messages
    withChatTemplate:(NSString *)chatTemplate
    withJsonSchema:(NSString *)jsonSchema
    withTools:(NSString *)tools
    withParallelToolCalls:(BOOL)parallelToolCalls
    withToolChoice:(NSString *)toolChoice;
- (NSString *)getFormattedChat:(NSString *)messages withChatTemplate:(NSString *)chatTemplate;
- (NSDictionary *)loadSession:(NSString *)path;
- (int)saveSession:(NSString *)path size:(int)size;
- (NSString *)bench:(int)pp tg:(int)tg pl:(int)pl nr:(int)nr;
- (void)applyLoraAdapters:(NSArray *)loraAdapters;
- (void)removeLoraAdapters;
- (NSArray *)getLoadedLoraAdapters;
- (void)clearCache:(BOOL)clearData;
- (void)invalidate;

@end
