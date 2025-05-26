export interface CapacitorLlamaPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
