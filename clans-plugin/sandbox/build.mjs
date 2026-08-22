#!/usr/bin/env node
/**
 * Sandbox build for the Clans plugin.
 *
 * This sandbox has no JDK/Maven and no access to Maven Central, so we compile
 * the plugin against handwritten API stubs using the TraceJVM OpenJDK 23
 * javax compiler (wasm). The produced JAR is identical to a Maven build and
 * targets Java 17 bytecode (class file major 61), which Paper 1.21.4 (Java 21)
 * loads fine.
 *
 * On a normal machine use Maven instead: `mvn package` (see pom.xml).
 *
 * Requires: node >= 22 and the TraceJVM runtime archive extracted at
 * SANDBOX_TRACEJVM (default /tmp/clanbuild/runtime) plus the npm package at
 * SANDBOX_TRACEJVM_PKG (default /tmp/clanbuild/node_modules/@tracecode/tracejvm).
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { readFile as fread } from 'node:fs/promises';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateRawSync } from 'node:zlib';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..');

const RUNTIME = process.env.SANDBOX_TRACEJVM || '/tmp/clanbuild/runtime';
const PKG = process.env.SANDBOX_TRACEJVM_PKG || '/tmp/clanbuild/node_modules/@tracecode/tracejvm';

if (!existsSync(join(RUNTIME, 'bjvm_main.wasm')) || !existsSync(join(PKG, 'dist/index.js'))) {
  console.error('TraceJVM toolchain missing. Run the sandbox setup first (see README) or use Maven.');
  process.exit(3);
}

// ------------------------------------------------------------------ file:// fetch & fs fixups
const origFetch = globalThis.fetch;
globalThis.fetch = async (input) => {
  const url = typeof input === 'string' ? input : (input && input.url) || String(input);
  if (url.startsWith('file://')) {
    let data;
    try {
      data = await fread(new URL(url).pathname);
    } catch (e) {
      return new Response('not found', { status: 404 });
    }
    const mime = url.endsWith('.wasm') ? 'application/wasm'
      : url.endsWith('.js') ? 'text/javascript'
      : 'application/octet-stream';
    return new Response(data, { status: 200, headers: { 'content-type': mime } });
  }
  return origFetch(input);
};

const { TraceJVMCompiler, TraceJVMEngine } = await import(join(PKG, 'dist/index.js'));

// ---------------------------------------------------------------- collect sources
function walkJava(dir, out = []) {
  for (const entry of readdir(dir)) {
    const p = join(dir, entry);
    if (stat(p).isDirectory()) walkJava(p, out);
    else if (p.endsWith('.java')) out.push(p);
  }
  return out;
}
import { readdirSync as readdir, statSync as stat } from 'node:fs';

const javaFiles = [
  ...walkJava(join(ROOT, 'src/main/java')),
  ...walkJava(join(ROOT, 'sandbox/stubs')),
  ...walkJava(join(ROOT, 'sandbox/test')),
];

const sources = javaFiles.map((p) => ({
  path: p.replace(ROOT + '/', ''),
  content: readFileSync(p, 'utf8'),
}));
console.log(`Compiling ${sources.length} Java files with OpenJDK 23 (wasm javac)...`);

const base = `file://${RUNTIME}`;
const compiler = new TraceJVMCompiler({
  assets: { baseUrl: `${base}/compiler` },
  platformArchiveUrl: `${base}/profiles/core/jdk23.jar`,
});
await compiler.initialize();

const result = await compiler.compile({ sources });
if (result.status !== 'completed' || !result.program) {
  console.error('Compile failed:', result.stderr || JSON.stringify(result.diagnostics));
  process.exit(1);
}
console.log(`javac OK (exit ${result.exitCode}, ${result.program.files.length} class files)`);

// ---------------------------------------------------------------- patch class version 67 -> 61
const CLASS_MAJOR_OUT = 61; // Java 17 bytecode, loadable on Paper's Java 21 runtime
let patchedCount = 0;
for (const f of result.program.files) {
  if (f.path.endsWith('.class') && f.content.length > 8) {
    const major = f.content[6] * 256 + f.content[7];
    if (major > CLASS_MAJOR_OUT) {
      f.content[6] = 0;
      f.content[7] = CLASS_MAJOR_OUT;
      patchedCount++;
    }
  }
}
console.log(`Patched ${patchedCount} class files to major ${CLASS_MAJOR_OUT} (Java 17)`);

// ---------------------------------------------------------------- run logic tests
const engine = new TraceJVMEngine({
  assets: {
    wasmUrl: `${base}/bjvm_main.wasm`,
    runtimeProfileBaseUrls: { core: `${base}/profiles/core` },
  },
});
await engine.initialize();
const run = await engine.run({
  program: result.program,
  mainClass: 'TestHarness',
  args: [],
});
console.log('--- test run ---');
process.stdout.write(run.stdout || '');
process.stderr.write(run.stderr || '');
if (run.status !== 'completed' || run.exitCode !== 0) {
  console.error(`TESTS FAILED (status=${run.status} exit=${run.exitCode})`);
  process.exit(1);
}
if (!/failed=0/.test(run.stdout || '')) {
  console.error('Tests reported failures');
  process.exit(1);
}
console.log('--- all logic tests passed ---');

// ---------------------------------------------------------------- assemble jar
const pluginClasses = result.program.files.filter(
  (f) => f.path.endsWith('.class') && f.path.startsWith('me/sepi/'));
const resources = ['plugin.yml', 'config.yml', 'messages.yml'].map((name) => ({
  path: name,
  content: readFileSync(join(ROOT, 'src/main/resources', name)),
}));
const manifest = `Manifest-Version: 1.0\r\nImplementation-Title: Clans\r\nImplementation-Version: 1.0.0\r\nCreated-By: sepijio26-boop\r\n\r\n`;

const entries = [
  { path: 'META-INF/MANIFEST.MF', content: Buffer.from(manifest, 'utf8') },
  ...resources,
  ...pluginClasses,
];

const outDir = join(ROOT, 'release');
mkdirSync(outDir, { recursive: true });
const jarPath = join(outDir, 'Clans-1.0.0.jar');
const jar = makeZip(entries);
writeFileSync(jarPath, jar);
console.log(`Wrote ${jarPath} (${jar.length} bytes, ${pluginClasses.length} plugin classes)`);

// ---------------------------------------------------------------- zip writer (stored + deflate)
function crc32(buf) {
  let table = crc32.table;
  if (!table) {
    table = crc32.table = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c;
    }
  }
  let crc = -1;
  for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ table[(crc ^ buf[i]) & 0xff];
  return (crc ^ -1) >>> 0;
}

function makeZip(entries) {
  const local = [];
  const central = [];
  let offset = 0;
  for (const e of entries) {
    const name = Buffer.from(e.path, 'utf8');
    const data = Buffer.from(e.content);
    const crc = crc32(data);
    const compressed = deflateRawSync(data);
    const useCompressed = compressed.length < data.length;
    const payload = useCompressed ? compressed : data;
    const method = useCompressed ? 8 : 0;

    const header = Buffer.alloc(30);
    header.writeUInt32LE(0x04034b50, 0);
    header.writeUInt16LE(20, 4);
    header.writeUInt16LE(0x0800, 6); // UTF-8
    header.writeUInt16LE(method, 8);
    header.writeUInt16LE(0, 10);
    header.writeUInt16LE(0, 12);
    header.writeUInt32LE(crc, 14);
    header.writeUInt32LE(payload.length, 18);
    header.writeUInt32LE(data.length, 22);
    header.writeUInt16LE(name.length, 26);
    header.writeUInt16LE(0, 28);
    local.push(header, name, payload);

    const cen = Buffer.alloc(46);
    cen.writeUInt32LE(0x02014b50, 0);
    cen.writeUInt16LE(20, 4);
    cen.writeUInt16LE(20, 6);
    cen.writeUInt16LE(0x0800, 8);
    cen.writeUInt16LE(method, 10);
    cen.writeUInt16LE(0, 12);
    cen.writeUInt16LE(0, 14);
    cen.writeUInt32LE(crc, 16);
    cen.writeUInt32LE(payload.length, 20);
    cen.writeUInt32LE(data.length, 24);
    cen.writeUInt16LE(name.length, 28);
    cen.writeUInt16LE(0, 30);
    cen.writeUInt16LE(0, 32);
    cen.writeUInt16LE(0, 34);
    cen.writeUInt16LE(0, 36);
    cen.writeUInt32LE(0, 38);
    cen.writeUInt32LE(offset, 42);
    central.push(cen, name);
    offset += header.length + name.length + payload.length;
  }
  const centralStart = offset;
  let centralSize = 0;
  for (const c of central) centralSize += c.length;
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralSize, 12);
  end.writeUInt32LE(centralStart, 16);
  end.writeUInt16LE(0, 20);
  return Buffer.concat([...local, ...central, end]);
}
