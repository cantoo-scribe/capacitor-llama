#import <Foundation/Foundation.h>

@interface CAPLlama : NSObject 

+ (void)toggleNativeLog:(BOOL)enabled;
+ (void)setContextLimit:(double)limit;
+ (NSDictionary *)modelInfo:(NSString *)path withSkip:(NSArray *)skip;
+ (NSDictionary *)initContext:(double)contextId withContextParams:(NSDictionary *)contextParams;
+ (NSDictionary *)getFormattedChat:(double)contextId withMessages:(NSString *)messages withTemplate:(NSString *)chatTemplate withParams:(NSDictionary *)params;
+ (NSDictionary *)loadSession:(double)contextId withFilePath:(NSString *)filePath;
+ (int)saveSession:(double)contextId withFilePath:(NSString *)filePath withSize:(double)size;
+ (NSDictionary *)completion:(double)contextId withCompletionParams:(NSDictionary *)completionParams;
+ (void)stopCompletion:(double)contextId;
+ (NSDictionary *)tokenize:(double)contextId text:(NSString *)text;
+ (NSString *)detokenize:(double)contextId tokens:(NSArray *)tokens;
+ (NSDictionary *)embedding:(double)contextId text:(NSString *)text params:(NSDictionary *)params;
+ (NSString *)bench:(double)contextId pp:(int)pp tg:(int)tg pl:(int)pl nr:(int)nr;
+ (void)applyLoraAdapters:(double)contextId withLoraAdapters:(NSArray *)loraAdapters;
+ (void)removeLoraAdapters:(double)contextId;
+ (NSArray *)getLoadedLoraAdapters:(double)contextId;
+ (void)releaseContext:(double)contextId;
+ (void)releaseAllContexts;

@end

