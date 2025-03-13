package client.connection;

public class ID {
    public static int AES(){return -5;}

    public static int ALIVE(){
        return -1;
    }

    public static int COMMAND(){
        return 0;
    }

    public static int STRING(){
        return 1;
    }

    public static int MESSAGE(){
        return 2;
    }

    public static int CMD_OUT(){
        return 3;
    }

    public static int SHELL_OUT(){
        return 4;
    }

    public static int SCREEN(){
        return 5;
    }

    public static int CAMERA(){
        return 6;
    }

    public static int COOKIE(){
        return 7;
    }

    public static int FILE_DIR(){
        return 8;
    }

    public static int FILE_IN(){
        return 9;
    }

    public static int FILE_OUT(){
        return 10;
    }

    public static int CHROME_COOKIE(){
        return 101;
    }
    public static int EDGE_COOKIE(){
        return 102;
    }
    public static int IS_QQ_NT(){
        return 11;
    }
    public static int IS_QQ_OLD(){
        return 12;
    }
    public static int NO_QQ(){
        return 13;
    }
    public static int QQ_DATA(){
        return 14;
    }

    public static int KEYBOARD(){return 15;}
    public static int FILE_EXISTS() { return 16; }
    public static int FILE_UPLOAD_DECISION() { return 17; }
    public static int FILE_UPLOAD_SUCCESS() { return 18; }
    public static int FILE_SIZE() { return 19; }
}
