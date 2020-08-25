##### Vad-Java 



本小工具用来切割wav 音频文件。 可以将音频中的空白部分剪切掉，然后根据参数输出有声音的小片段音频。



使用方法， clone下来之后直接打包，只有一个参数包依赖，其他依赖都没有。



可以使用 -h 参数 查看参数说明。



```shell
java -jar  audiotools-1.0-SNAPSHOT-jar-with-dependencies.jar -h

usage: AudioToolCommonCLI
 -d,--outputDir <arg>            拆分合并之后的小文件输出路径
 -f,--audiofile <arg>            输入的音频文件，只支持单通道，wav格式
 -h,--help                       参数帮助
 -om,--oneStepMergedSec <arg>    单次合并生成的音频的最短时长下限，单位为秒
 -op,--oneStepProcessSec <arg>   单批次处理的音频的时长，单位为秒
```





运行命令举例：

```shell
java -jar  audiotools-1.0-SNAPSHOT-jar-with-dependencies.jar -f /Users/zyw/Downloads/7470703161602011632.wav -d /tmp/test
```