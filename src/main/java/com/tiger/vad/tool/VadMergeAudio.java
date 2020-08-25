package com.tiger.vad.tool;

/**
 * Created with Idea
 * User: zyw
 * Date: 2020/8/25
 * Time: 11:11 下午
 * 用来通过vad 检测然后合并的工具类
 **/



import com.tiger.vad.energy.Vad;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;


public class VadMergeAudio {

    private File audioFile;
    private Integer channel;
    private Integer sampleRate;
    private Integer frameByteSize;
    private Vad vad;
    private AudioFormat audioFormat;
    private AudioFileFormat.Type targetFileType;
    private String outputDir;
    String fileNamePrefix;

    private int outputIndex = 0;

    //单次用来处理源文件的片段缓存
    private ByteArrayOutputStream tmpRawAudioFileByteArrayCache;

    private ByteArrayOutputStream tmpMergedAudioFileByteArrayCache;

    private Integer oneStepProcessBytesThreshould; // 这个通过时长和文件的采样率来计算得到
    private Integer oneStepProcessSec = 15; //单次处理的源音频为15秒  //控制不能大于120秒防止太大
    private Integer oneStepMergedSec = 10; //合并池，等到满10秒之后输出
    private Integer oneStepMergedBytesThreshould;


    private int samplePer10MilSec;
    private int samplePer25MilSec;
    private int bytePer25MilSec;
    private int bytePer10MilSec;


    public VadMergeAudio(File audioFile, Integer oneStepProcessSec, Integer oneStepMergedSec,String outputDir) {
        this.audioFile = audioFile;

        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(this.audioFile);
            this.fileNamePrefix = this.audioFile.getName().substring(0,this.audioFile.getName().lastIndexOf("."));
            this.targetFileType = fileFormat.getType();
            this.audioFormat = fileFormat.getFormat();
            this.channel = audioFormat.getChannels();
            this.frameByteSize = audioFormat.getFrameSize();
            this.sampleRate = (int)audioFormat.getSampleRate();

            this.oneStepProcessSec = (oneStepProcessSec == null || oneStepProcessSec ==0) ? this.oneStepProcessSec : oneStepProcessSec;
            this.oneStepMergedSec = (oneStepMergedSec == null || oneStepMergedSec == 0 ) ? this.oneStepMergedSec : oneStepMergedSec;

            this.oneStepProcessBytesThreshould = this.oneStepProcessSec * this.sampleRate * this.frameByteSize;
            this.oneStepMergedBytesThreshould = this.oneStepMergedSec * this.sampleRate * this.frameByteSize;
            this.vad = new Vad();
            this.outputDir = outputDir;
            this.tmpRawAudioFileByteArrayCache = new ByteArrayOutputStream(this.oneStepProcessBytesThreshould*2);
            this.tmpMergedAudioFileByteArrayCache = new ByteArrayOutputStream(this.oneStepMergedBytesThreshould * 2);

            this.samplePer10MilSec = this.sampleRate * 10 / 1000;
            this.samplePer25MilSec = this.sampleRate * 25 / 1000;
            this.bytePer25MilSec = samplePer25MilSec * this.frameByteSize;
            this.bytePer10MilSec = this.samplePer10MilSec * this.frameByteSize;

            System.out.println(this.toString());

        }catch (Exception e){
            System.err.println("VideoCut 初始化错误!" + e.getMessage());
        }
    }

    public VadMergeAudio(String audioFile, Integer oneStepProcessSec, Integer oneStepMergedSec,String outputDir) {
        this(new File(audioFile),oneStepProcessSec,oneStepMergedSec,outputDir);
    }


    public VadMergeAudio(String audioFile) {
        this(new File(audioFile),15,10,"/tmp");
    }

    public synchronized void process() throws Exception{

        AudioInputStream inputStream = AudioSystem.getAudioInputStream(this.audioFile);
        Integer bufferSize = 1024 * this.frameByteSize;

        byte[] byteBuffer = new byte[bufferSize];

        while(true){
            int numBytesRead = inputStream.read(byteBuffer);
            if(numBytesRead != -1){
                tmpRawAudioFileByteArrayCache.write(byteBuffer,0,numBytesRead);
            }else{
                break;
            }

            if( tmpRawAudioFileByteArrayCache.size() > this.oneStepProcessBytesThreshould || ( numBytesRead == -1 && tmpRawAudioFileByteArrayCache.size() > this.oneStepProcessBytesThreshould/2)){
                processOneByffer(this.tmpRawAudioFileByteArrayCache.toByteArray());
                this.tmpRawAudioFileByteArrayCache.reset();
            }

            if(this.tmpMergedAudioFileByteArrayCache.size() > this.oneStepMergedBytesThreshould){
                flushMergedAudioFile();
            }
        }

        if(this.tmpMergedAudioFileByteArrayCache.size() > 0 ){
            flushMergedAudioFile();
        }

        inputStream.close();
    }



    private synchronized void processOneByffer(byte[] rawAudioByteArray) throws Exception{

        int numShiftSteps;

        if(bytePer25MilSec > rawAudioByteArray.length){
            throw new Exception("audio file is too short");
        }
        numShiftSteps = (rawAudioByteArray.length - bytePer25MilSec) / bytePer10MilSec +1;

        for(int i = 0; i< numShiftSteps; i ++){
            //读到头了
            if(i*bytePer10MilSec +samplePer25MilSec > rawAudioByteArray.length) break;

            float [] vadFloatArray = new float[samplePer25MilSec];
            int downBound = bytePer10MilSec * i;
            for(int index = 0; index < samplePer25MilSec; index ++){
                byte b1 = rawAudioByteArray[downBound + index*2];
                byte b2 = rawAudioByteArray[downBound + index*2+1];
                //由于这里是 little-endian 所以他是低位在前，高位在后。所以需要把后面的一个byte左移8位到高位
                vadFloatArray[index] = (short)(b2 << 8 | b1);
            }

            boolean isSpeech = this.vad.isSpeech(vadFloatArray);

            if(isSpeech){
                this.tmpMergedAudioFileByteArrayCache.write(rawAudioByteArray,downBound,bytePer10MilSec);
            }
        }

    }



    public synchronized void flushMergedAudioFile() throws Exception{

        byte[] speechByte = this.tmpMergedAudioFileByteArrayCache.toByteArray();
        ByteArrayInputStream speechInputStream = new ByteArrayInputStream(speechByte);
        AudioInputStream outputAudioInputSteram = new AudioInputStream(speechInputStream, audioFormat, speechByte.length / audioFormat.getFrameSize());
        File outPutFile = Paths.get(this.outputDir,this.fileNamePrefix+ "_"+ String.valueOf(outputIndex++)+ ".wav").toFile();
        System.out.println("生成新的文件：" + outPutFile.toString());
        AudioSystem.write(outputAudioInputSteram, this.targetFileType, outPutFile);
        this.tmpMergedAudioFileByteArrayCache.reset();

    }


    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public Integer getOneStepProcessSec() {
        return oneStepProcessSec;
    }

    public void setOneStepProcessSec(Integer oneStepProcessSec) {
        this.oneStepProcessSec = oneStepProcessSec;
    }

    public Integer getOneStepMergedSec() {
        return oneStepMergedSec;
    }

    public void setOneStepMergedSec(Integer oneStepMergedSec) {
        this.oneStepMergedSec = oneStepMergedSec;
    }

    @Override
    public String toString() {
        return "VadMergeAudio{" +
                "audioFile=" + audioFile +
                ", channel=" + channel +
                ", sampleRate=" + sampleRate +
                ", frameByteSize=" + frameByteSize +
                ", audioFormat=" + audioFormat +
                ", targetFileType=" + targetFileType +
                ", oneStepProcessSec=" + oneStepProcessSec +
                ", oneStepMergedSec=" + oneStepMergedSec +
                '}';
    }
}