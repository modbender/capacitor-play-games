// `bun run docgen` is broken on this toolchain — @capacitor/docgen 0.3.1 pins
// `typescript: ~4.2.4` as a hard dependency, and bun installs it nested, so
// the root TypeScript 7 compiler is never consulted. Regenerating the
// README's API section by hand (as this fork currently does) has no
// automatic check that every method got a matching doc entry, so this
// script is a narrow substitute: it diffs the method names declared on
// `PlayGamesPlugin` against the names README.md documents.
//
// Limit, by design: this checks *names* only — that every interface method
// has a docgen-index entry and a matching `### ` heading, and that neither
// list names a method the interface doesn't have. It does not read prose,
// so a heading present with stale or wrong text underneath it still passes.

import { readFileSync } from "node:fs";
import { join } from "node:path";

const root = join(import.meta.dir, "..");
const definitionsPath = join(root, "src", "definitions.ts");
const readmePath = join(root, "README.md");

const definitionsSrc = readFileSync(definitionsPath, "utf8");
const readmeSrc = readFileSync(readmePath, "utf8");

function section(src: string, startMarker: string, endMarker: string, label: string): string {
  const start = src.indexOf(startMarker);
  const end = src.indexOf(endMarker, start + startMarker.length);
  if (start === -1 || end === -1) {
    throw new Error(`check-docs: could not find ${label} in the source it was read from`);
  }
  return src.slice(start + startMarker.length, end);
}

const pluginInterface = section(
  definitionsSrc,
  "export interface PlayGamesPlugin {",
  "\n}",
  "the PlayGamesPlugin interface body",
);

const interfaceMethods = [...pluginInterface.matchAll(/^ {2}(\w+)\(/gm)].map((m) => m[1]);
if (interfaceMethods.length === 0) {
  throw new Error("check-docs: found zero methods on PlayGamesPlugin — the extraction regex is broken");
}

const docgenIndex = section(readmeSrc, "<docgen-index>", "</docgen-index>", "the README's <docgen-index> block");
const docgenApi = section(readmeSrc, "<docgen-api>", "</docgen-api>", "the README's <docgen-api> block");

const indexedNames = new Set(
  [...docgenIndex.matchAll(/^\* \[`([^`]+)`\]\(#[^)]+\)$/gm)]
    .map((m) => m[1].match(/^\w+/)?.[0])
    .filter((name): name is string => !!name),
);

const headedNames = new Set(
  [...docgenApi.matchAll(/^### (\w+)\(/gm)].map((m) => m[1]),
);

const errors: string[] = [];

for (const name of interfaceMethods) {
  if (!indexedNames.has(name)) {
    errors.push(`"${name}" is on PlayGamesPlugin but has no <docgen-index> entry in README.md`);
  }
  if (!headedNames.has(name)) {
    errors.push(`"${name}" is on PlayGamesPlugin but has no "### ${name}(" heading in README.md`);
  }
}

const methodSet = new Set(interfaceMethods);
for (const name of indexedNames) {
  if (!methodSet.has(name)) {
    errors.push(`README.md's <docgen-index> documents "${name}", which is not a method on PlayGamesPlugin`);
  }
}
for (const name of headedNames) {
  if (!methodSet.has(name)) {
    errors.push(`README.md has a "### ${name}(" heading, but "${name}" is not a method on PlayGamesPlugin`);
  }
}

if (errors.length > 0) {
  console.error("check-docs: README.md and src/definitions.ts have drifted:\n");
  for (const error of errors) console.error(`  - ${error}`);
  console.error("\nUpdate README.md (or definitions.ts) to match, then rerun.");
  process.exit(1);
}

console.log(`check-docs: README.md documents all ${interfaceMethods.length} PlayGamesPlugin methods, and no others.`);
