import commonjs from '@rollup/plugin-commonjs';
// import nodeResolve from '@rollup/plugin-node-resolve';
import resolve from '@rollup/plugin-node-resolve';
import builtins from 'rollup-plugin-node-builtins';
import globals from 'rollup-plugin-node-globals';

export default {
    input: 'electron/build/electron/src/index.js',
    output: [
      {
        file: 'electron/dist/plugin.js',
        format: 'cjs',
        sourcemap: true,
        inlineDynamicImports: true,
      },
    ],
  plugins: [
    globals(),
    builtins(),
    resolve({
      preferBuiltins: true
    }),
    commonjs()
  ],
  external: ['path', 'fs', 'os'] // <-- Add node built-in modules here
  };
// export default {
//   input: 'electron/build/electron/src/index.js',
//   output: [
//     {
//       file: 'electron/dist/plugin.js',
//       format: 'cjs',
//       sourcemap: true,
//       inlineDynamicImports: true,
//     },
//   ],
//   external: ['@capacitor/core'],
// };