import { registerPlugin } from "@capacitor/core";

import type { PlayGamesPlugin } from "./definitions";

const PlayGames = registerPlugin<PlayGamesPlugin>("PlayGames", {
  web: () => import("./web").then((m) => new m.PlayGamesWeb()),
});

export * from "./definitions";
export { PlayGames };
