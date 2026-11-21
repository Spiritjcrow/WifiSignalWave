package com.wifisignal.wave.renderer;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Animated illustrative 3D signal wave. Not measured spatial RF geometry. */
public final class SignalRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "WifiSignalWave";
    private static final int GRID = 48;
    private static final int COUNT = GRID * GRID;
    private static final String VERTEX =
            "#version 300 es\n" +
            "uniform mat4 uMVP; uniform float uTime; uniform float uStrength; uniform float uBand;\n" +
            "layout(location=0) in vec3 vPosition; out float vIntensity;\n" +
            "void main(){\n" +
            "  float d=length(vPosition.xz);\n" +
            "  float wave=sin(d*(7.0+uBand*2.0)-uTime*4.0);\n" +
            "  float lift=wave*exp(-d*0.20)*uStrength*0.70;\n" +
            "  vIntensity=clamp(0.15+uStrength*0.65+lift*0.22,0.0,1.0);\n" +
            "  gl_Position=uMVP*vec4(vPosition.x,lift,vPosition.z,1.0);\n" +
            "  gl_PointSize=3.0+vIntensity*8.0;\n" +
            "}\n";
    private static final String FRAGMENT =
            "#version 300 es\n" +
            "precision mediump float; in float vIntensity; out vec4 fragColor;\n" +
            "void main(){\n" +
            "  vec2 p=gl_PointCoord-vec2(0.5);\n" +
            "  float r=length(p); if(r>0.5) discard;\n" +
            "  float glow=1.0-smoothstep(0.08,0.5,r);\n" +
            "  vec3 blue=vec3(0.02,0.22,0.80), cyan=vec3(0.00,0.90,1.00);\n" +
            "  vec3 green=vec3(0.00,1.00,0.48), red=vec3(1.00,0.18,0.36);\n" +
            "  vec3 c=vIntensity<0.33 ? mix(blue,cyan,vIntensity/0.33)\n" +
            "    : vIntensity<0.70 ? mix(cyan,green,(vIntensity-0.33)/0.37)\n" +
            "    : mix(green,red,(vIntensity-0.70)/0.30);\n" +
            "  fragColor=vec4(c*glow,0.82*glow);\n" +
            "}\n";
    private final float[] projection=new float[16], view=new float[16], mvp=new float[16];
    private int program, vao, mvpLoc, timeLoc, strengthLoc, bandLoc;
    private long started;
    private float zoom=1f, pitch=45f, yaw=0f, strength=0f, band=1f;

    /** Call updates on GLSurfaceView's GL thread via queueEvent. */
    public void updateSignalData(float normalized, int frequencyMhz) {
        strength=Math.max(0f,Math.min(1f,normalized));
        band=frequencyMhz>=5925?1.3f:frequencyMhz>=4900?1.1f:0.8f;
    }
    public void setZoom(float factor) {
        zoom=Math.max(0.35f,Math.min(3.5f,zoom*factor));
    }
    public void rotate(float dx,float dy) {
        yaw+=dx*0.30f;
        pitch=Math.max(12f,Math.min(78f,pitch+dy*0.18f));
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg) {
        started=SystemClock.uptimeMillis();
        GLES30.glClearColor(0.02f,0.02f,0.08f,1f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE_MINUS_SRC_ALPHA);
        program=makeProgram();
        if(program!=0) {
            mvpLoc=GLES30.glGetUniformLocation(program,"uMVP");
            timeLoc=GLES30.glGetUniformLocation(program,"uTime");
            strengthLoc=GLES30.glGetUniformLocation(program,"uStrength");
            bandLoc=GLES30.glGetUniformLocation(program,"uBand");
            createGrid();
        }
    }
    private int shader(int type,String src) {
        int id=GLES30.glCreateShader(type);
        GLES30.glShaderSource(id,src);
        GLES30.glCompileShader(id);
        int[] status=new int[1];
        GLES30.glGetShaderiv(id,GLES30.GL_COMPILE_STATUS,status,0);
        if(status[0]==0) {
            Log.e(TAG,"Shader error: "+GLES30.glGetShaderInfoLog(id));
            GLES30.glDeleteShader(id);
            return 0;
        }
        return id;
    }
    private int makeProgram() {
        int v=shader(GLES30.GL_VERTEX_SHADER,VERTEX),f=shader(GLES30.GL_FRAGMENT_SHADER,FRAGMENT);
        if(v==0||f==0) {
            if(v!=0)GLES30.glDeleteShader(v);
            if(f!=0)GLES30.glDeleteShader(f);
            return 0;
        }
        int id=GLES30.glCreateProgram();
        GLES30.glAttachShader(id,v);
        GLES30.glAttachShader(id,f);
        GLES30.glLinkProgram(id);
        GLES30.glDeleteShader(v);
        GLES30.glDeleteShader(f);
        int[] status=new int[1];
        GLES30.glGetProgramiv(id,GLES30.GL_LINK_STATUS,status,0);
        if(status[0]==0) {
            Log.e(TAG,"Link error: "+GLES30.glGetProgramInfoLog(id));
            GLES30.glDeleteProgram(id);
            return 0;
        }
        return id;
    }
    private void createGrid() {
        float[] vertices=new float[COUNT*3];
        int i=0;
        for(int x=0;x<GRID;x++)for(int z=0;z<GRID;z++){
            vertices[i++]=(x-(GRID-1f)/2f)*0.14f;
            vertices[i++]=0f;
            vertices[i++]=(z-(GRID-1f)/2f)*0.14f;
        }
        FloatBuffer data=ByteBuffer.allocateDirect(vertices.length*Float.BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        data.put(vertices).position(0);
        int[] ids=new int[1];
        GLES30.glGenVertexArrays(1,ids,0);
        vao=ids[0];
        GLES30.glBindVertexArray(vao);
        GLES30.glGenBuffers(1,ids,0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,ids[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertices.length*Float.BYTES,data,GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3*Float.BYTES,0);
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
        GLES30.glViewport(0,0,w,h);
        Matrix.perspectiveM(projection,0,55f,(float)Math.max(1,w)/Math.max(1,h),0.1f,80f);
    }
    @Override public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT|GLES30.GL_DEPTH_BUFFER_BIT);
        if(program==0)return;
        float rad=(float)Math.toRadians(yaw),distance=8f/zoom;
        float x=(float)Math.sin(rad)*distance,z=(float)Math.cos(rad)*distance;
        float y=3f+(pitch-45f)*0.065f;
        Matrix.setLookAtM(view,0,x,y,z,0f,0f,0f,0f,1f,0f);
        Matrix.multiplyMM(mvp,0,projection,0,view,0);
        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);
        GLES30.glUniform1f(timeLoc,(SystemClock.uptimeMillis()-started)/1000f);
        GLES30.glUniform1f(strengthLoc,strength);
        GLES30.glUniform1f(bandLoc,band);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_POINTS,0,COUNT);
        GLES30.glBindVertexArray(0);
    }
}
