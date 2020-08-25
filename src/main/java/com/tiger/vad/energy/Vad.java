package com.tiger.vad.energy;

/**
 * Created with Idea
 * User: zyw
 * Date: 2020/8/25
 * Time: 11:10 下午
 **/


public class Vad {

    public enum VadState{
        speech,silence;
    }

    private float energyThreshold = 1.5e7f;

    //参考数值3
    private int silenceToSpeechThreshold = 5;

    //参考数值 100 这里设大一点可以增大容忍度
    private int speechToSilenceThreshold = 100;

    private int silenceFrameCount = 0;
    private int speechFrameCount = 0;
    private VadState vadState = VadState.silence;

    public Vad(float energyThreshold, int silenceToSpeechThreshold, int speechToSilenceThreshold) {
        this.energyThreshold = energyThreshold;
        this.silenceToSpeechThreshold = silenceToSpeechThreshold;
        this.speechToSilenceThreshold = speechToSilenceThreshold;
    }

    public Vad() {}

    public void reset(){
        this.silenceFrameCount = 0;
        this.speechFrameCount = 0;
        this.vadState = VadState.silence;
    }


    public boolean isSpeech(float[] data){

        float energy = 0.0f;
        boolean isVoice = false;

        for(int i = 0 ; i<data.length; i++){
            energy += data[i] * data[i];
        }

        if (energy > this.energyThreshold) isVoice = true;

        switch (vadState){

            case silence:
                if(isVoice){
                    this.speechFrameCount++;
                    if(this.speechFrameCount >= this.silenceToSpeechThreshold){
                        this.vadState = VadState.speech;
                        this.silenceFrameCount = 0;
                    }
                }else{
                    this.speechFrameCount = 0;
                }
                break;

            case speech:
                if(!isVoice){
                    this.silenceFrameCount++;
                    if( this.silenceFrameCount >= this.speechToSilenceThreshold ){
                        this.vadState = VadState.silence;
                        this.speechFrameCount = 0;
                    }
                }else{
                    this.silenceFrameCount = 0;
                }
                break;
        }

        if(this.vadState.equals(VadState.speech)){
            return true;
        }else{
            return false;
        }

    }


}
