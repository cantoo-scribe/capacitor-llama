require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

base_ld_flags = "-framework Accelerate -framework Foundation -framework Metal -framework MetalKit"
base_compiler_flags = "-fmodules -fcxx-modules -fno-objc-arc -DLM_GGML_USE_CPU -DLM_GGML_USE_ACCELERATE -Wno-shorten-64-to-32"

if ENV["LLAMA_DISABLE_METAL"] != "1" then
  base_compiler_flags += " -DLM_GGML_USE_METAL -DLM_GGML_METAL_USE_BF16" # -DLM_GGML_METAL_NDEBUG
end

# Use base_optimizer_flags = "" for debug builds
# base_optimizer_flags = ""
base_optimizer_flags = "-O3 -DNDEBUG"

Pod::Spec.new do |s|
  s.name = 'CantooCapacitorLlama'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Sources/**/*.{swift,h,m,mm,hpp}'
  s.vendored_frameworks = "ios/Sources/CapacitorLlamaPlugin/capllama.xcframework"
  s.prepare_command = <<-SCRIPT
    FRAMEWORK_PATH="ios/Sources/CapacitorLlamaPlugin/capllama.xcframework"
    RELEASE_URL="https://github.com/cantoo-scribe/capacitor-llama/releases/download/v0.0.1/capllama.xcframework.zip"

    if [ ! -d "$FRAMEWORK_PATH" ]; then
      echo "📦 Downloading capllama.xcframework..."
      curl -L "$RELEASE_URL" -o /tmp/capllama.xcframework.zip
      unzip -q /tmp/capllama.xcframework.zip -d "ios/Sources/CapacitorLlamaPlugin/"
      rm -f /tmp/capllama.xcframework.zip
      echo "✅ capllama.xcframework ready."
    fi
  SCRIPT
  s.public_header_files = "ios/Sources/CapacitorLlamaPlugin/CapacitorLlamaPlugin-Bridging-Header.h",
  s.ios.deployment_target = '14.0'
  s.dependency 'Capacitor'
  s.swift_version = '5.1'
  
  s.compiler_flags = base_compiler_flags
  s.pod_target_xcconfig = {
#    'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES',
    "OTHER_LDFLAGS" => base_ld_flags,
    "OTHER_CFLAGS" => base_optimizer_flags,
    "OTHER_CPLUSPLUSFLAGS" => base_optimizer_flags + " -std=c++17"
  }
  # install_modules_dependencies(s)
end
