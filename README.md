# Unpacker

Simple archive unpacking for Java.

## Installation

Releases of unpacker are available on the Central Repository. For manual installations, unpacker requires Apache Commons Compress, the SLF4J API, and our [annotations](https://git.wukl.net/f00f/annotations) library.

## Usage

Unpacker is meant to be used with dependency injection through its constructor. The current implementation expects settings to be passed to its constructor:

```java
final var settings = new UnpackerSettingsBuilder().build();
final var unpacker = new Unpacker(settings);
```

See the API documentation of UnpackerSettingsBuilder for the available parameters.

With the unpacker instance ready, archives can be unpacked as follows:

```java
final Collection<Path> files = unpacker.unpack(pathToArchiveFile, pathToTargetDirectory);
```

The returned collection contains the paths to all files that have been unpacked, rooted at the given path to the target directory. Unpacker also supports files that are not archives, in which case the single file is copied into the target directory.

The unpacker service is thread-safe and re-entrant, as long as the archive and target directory are not modified during unpacking.

---

![The destruction left by Spike Lee, 2013, colorized](https://git.wukl.net/uploads/-/system/project/avatar/103/unpacker+border.png)
