package client.utils;

import client.ClientMain;

public class TimerUtil {
    public long ticks = System.currentTimeMillis();
    public long delay;
    public boolean isTimeOn = true;

    public void setDelay(long ms){
        this.delay = ms;
    }

    public void start(){
        ticks = System.currentTimeMillis();
        new Thread(()->{
            while (true){
                if(ticks + delay <= System.currentTimeMillis()){

                    isTimeOn = true;
                    ticks = System.currentTimeMillis();
                }
            }
        }).start();
    }
}
