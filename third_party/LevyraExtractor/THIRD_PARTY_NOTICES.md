# Third-Party Notices

LevyraExtractor is a GPL-3.0 downstream extractor project.

## Core lineage

| Project | Repository | Role | License |
|---|---|---|---|
| NewPipeExtractor | https://github.com/TeamNewPipe/NewPipeExtractor | Original extractor architecture and service implementations | GPL-3.0 |
| PipePipeExtractor | https://github.com/InfinityLoop1308/PipePipeExtractor | Main downstream lineage used by LevyraExtractor | GPL-3.0 |
| MetrolistExtractor | https://github.com/MetrolistGroup/MetrolistExtractor | Reference implementation for modern multi-client YouTube stream handling | GPL-3.0 |

The multi-client Android VR, Safari and TVHTML5 resolution strategy and the aggregation of audio, muxed video and video-only formats were adapted from the open-source MetrolistExtractor approach and integrated into LevyraExtractor's existing SABR-aware architecture.

All original copyright headers remain in the relevant source files. The full project is distributed under GPL-3.0, and derivative distributions must preserve the corresponding license and notices.

## Libraries

LevyraExtractor also uses nanojson, jsoup, OkHttp, Wire, Protocol Buffers, Java-WebSocket, Brotli, Apache Commons, autolink, SpotBugs Annotations and other open-source dependencies declared in the Gradle build files.

Names and trademarks belong to their respective owners. LevyraExtractor is independent and is not affiliated with or endorsed by Google, YouTube, NewPipe, PipePipe or Metrolist.
