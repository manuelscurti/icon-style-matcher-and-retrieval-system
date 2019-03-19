# Icon Style Matcher and Retrieval System 
A content based retrieval system (CBIR) to find icons with similiar styles based on LIRE 

## Dependencies
- OPENCV
OpenCV library bindings for Java MUST be located at:
C:/opencv/build/java/x64/opencv_java401.dll
if not download opencv 4.0.1 (or >) from https://opencv.org/releases.html

- LUCENE
Download all the lucene binaries from https://lucene.apache.org/core/
and follow this tutorial
https://lucene.apache.org/core/8_0_0/demo/overview-summary.html#Setting_your_CLASSPATH
I suggest you to put lucene main folder into C:/

## Packing all together
1. Download the LIRE source code from www.lire-project.net
2. Copy src/ and res/ folders into LIRE main folder, if asked to join folders, say YES to all
3. Import the LIRE sources in any IDE that supports Gradle (e.g. Eclipse)
4. Export as JAR from the IDE

## Usage
1. Index:
	path/to/jar index path/to/images/folder
2. Search
	path/to/jar search path/to/image_to_search path/to/folder_for_saving_results

## Authors
- Manuel Scurti
- Jeanne Bosc-Bierne