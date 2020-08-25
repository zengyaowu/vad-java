package com.tiger.vad.main;

import com.tiger.vad.tool.VadMergeAudio;
import org.apache.commons.cli.*;

/**
 * java -jar  audiotools-1.0-SNAPSHOT-jar-with-dependencies.jar -f /Users/zyw/Downloads/7470703161602011632.wav -d /tmp/test
 * Created with Idea
 * User: zyw
 * Date: 2020/8/25
 * Time: 11:23 下午
 **/
public class EntryPoint {

    public static void main(String[] args) throws Exception {


        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        //commons-cli-1.2.jar com.tiger.vad.main.EntryPoint -f /Users/zyw/Downloads/output.wav -d /tmp/test
        options.addOption("h","help",false,"参数帮助");
        options.addOption("f","audiofile",true,"输入的音频文件，只支持单通道，wav格式");
        options.addOption("op","oneStepProcessSec",true,"单批次处理的音频的时长，单位为秒");
        options.addOption("om","oneStepMergedSec",true,"单次合并生成的音频的最短时长下限，单位为秒");
        options.addOption("d","outputDir",true,"拆分合并之后的小文件输出路径");

        CommandLine commandLine = parser.parse(options,args);

        if(commandLine.hasOption('h')){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "AudioToolCommonCLI", options );
            System.exit(0);
        }


        if(!commandLine.hasOption('f')){
            System.err.println("请输入参数： 需要处理的音频文件 -f");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "AudioToolCommonCLI", options );
            System.exit(0);
        }

        String audioFile = commandLine.getOptionValue("f");
        System.out.println("audio file is " + audioFile);


        VadMergeAudio vadMergeAudio = new VadMergeAudio(audioFile);

        if(commandLine.hasOption("op")){
            vadMergeAudio.setOneStepProcessSec(Integer.valueOf(commandLine.getOptionValue("op")));
        }
        if(commandLine.hasOption("om")){
            vadMergeAudio.setOneStepMergedSec(Integer.valueOf(commandLine.getOptionValue("om")));
        }
        if(commandLine.hasOption("d")){
            vadMergeAudio.setOutputDir(commandLine.getOptionValue('d'));
        }
        vadMergeAudio.process();

    }
}
