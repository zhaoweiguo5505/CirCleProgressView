package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.Nullable
import com.blankj.utilcode.util.SizeUtils
import java.lang.Exception
import java.util.*


/**
 * @Author: zhaoweiguo
 * @Date: 2022-05-05 10:59
 * @Description    首页连接wifi view
 */
class CircleProgressView : View {

    //刻度表渐变色
    val SWEEP_GRADIENT_COLORS = intArrayOf(Color.parseColor("#FFFFFF"),
        Color.parseColor("#CEDDFF"),
        Color.parseColor("#71A0FE"),
        Color.parseColor("#CEDDFF"),
        Color.parseColor("#ffffff"))
    // 进度条渐变色
    lateinit var PROGRESS_COLORS:IntArray

    private var tableWidth = SizeUtils.dp2px(30f)   //虚线宽度
    private var mPaint: Paint = Paint()  //刻度画笔
    private  var mPath: Path  //刻度路径





    private var freeTIme = "Free time"
    private var noConnect = "Connect"
    private var connecting = "ConnectIng"
    private lateinit var mTableRectF: RectF  //刻度条位置
    private lateinit var mProgressRectF:RectF //进度条rectF

    private var connectState = 0  // 连接状态 0   未连接    1  连接中 2  连接成功
    private  var mCircleWidth = 0f  //进度条宽度

    //把路径分成虚线段的
    private var dashPathEffect: DashPathEffect? = null

    //给路径上色
    private var mColorShader: SweepGradient? = null

    //指针的路径
    private var mPointerPath: Path? = null

    //圆环画笔
    private var mBgPaint:Paint = Paint()
    private var mBgColor:Int = 0
    private var mBgWidth = 0f

    //圆弧开始 圆点画笔
    private var mCirclePaint:Paint = Paint()
    //圆弧进度条画笔
    private var mProgressPaint:Paint = Paint()
    //圆弧渐变颜色
    private var mStartColor = 0
    private var mEndColor = 0

    //文字画笔
    private var textPaint  = Paint()
    private var textSize = 0f
    private var textColor = 0


    //圆弧进度条颜色
    private var mProgressColorShader : SweepGradient?=null

    //渐变旋转
    private lateinit var mMatrix:Matrix


    //获取文字高度
    private var textRect  = Rect()

    //进度条长度
    private var mProgressAngle = 0f


    private var isConnecting = false
    private var mHander =Handler()

    private var mRunnable =object :Runnable {
        override fun run() {
            if(isConnecting){
                mProgressAngle+=1
                if(mProgressAngle>=360){
                    mProgressAngle =0f
                }
                Log.i("测试一下", "run: ")
                mMatrix.postRotate(1f,width/2f,height/2f)
            }
            postInvalidate()
            mHander.postDelayed(this,100)
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs){
        val obtainStyledAttributes = context.obtainStyledAttributes(attrs,R.styleable.CircleProgressView)
        textSize    = obtainStyledAttributes.getDimensionPixelSize(R.styleable.CircleProgressView_cp_text_size,SizeUtils.dp2px(20f)).toFloat()
        textColor   = obtainStyledAttributes.getColor(R.styleable.CircleProgressView_cp_text_color,Color.parseColor("#9C27B0"))
        noConnect   = obtainStyledAttributes.getString(R.styleable.CircleProgressView_cp_noConnect_Text)?:"Connect"
        connecting  = obtainStyledAttributes.getString(R.styleable.CircleProgressView_cp_connecting_text)?:"Connecting"
        tableWidth  = obtainStyledAttributes.getDimensionPixelSize(R.styleable.CircleProgressView_cp_line_witdh,SizeUtils.dp2px(30f))
        mBgColor    = obtainStyledAttributes.getColor(R.styleable.CircleProgressView_cp_circle_color,Color.parseColor("#4080FD"))
        mBgWidth    = obtainStyledAttributes.getDimension(R.styleable.CircleProgressView_cp_circle_witdh,tableWidth.toFloat())
        mStartColor = obtainStyledAttributes.getColor(R.styleable.CircleProgressView_cp_circle_start_color,   Color.parseColor("#F0F3FF"))
        mEndColor   = obtainStyledAttributes.getColor(R.styleable.CircleProgressView_cp_circle_end_color, Color.parseColor("#71A0FE"))

        obtainStyledAttributes.recycle()
        initData()
    }

    private fun initData() {
        textPaint.textSize = textSize
        textPaint.color = textColor
        mBgPaint.color  = mBgColor
        mBgPaint.strokeWidth = mBgWidth
        mProgressPaint.strokeWidth = mBgWidth
        PROGRESS_COLORS = intArrayOf(mEndColor,mStartColor)
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)


    init {
        mPaint.setAntiAlias(true)
        mPaint.setDither(true)
        mPaint.setColor(Color.BLACK)
        mPath = Path()
        mMatrix = Matrix()
        mBgPaint.style = Paint.Style.STROKE
        mPointerPath = Path()
        mCirclePaint.style = Paint.Style.FILL
        mCirclePaint.setColor(Color.WHITE)

        textPaint.textAlign = Paint.Align.CENTER

        mProgressPaint.style = Paint.Style.STROKE
        mProgressPaint.strokeCap = Paint.Cap.ROUND
        mProgressPaint.strokeWidth = tableWidth.toFloat()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = (Math.min(w, h) - tableWidth * 2).toFloat()
        //刻度的位置方框
        mTableRectF = RectF(0f, 0f, size, size)
        mProgressRectF = RectF(tableWidth.toFloat()+SizeUtils.dp2px(10f),tableWidth.toFloat()+SizeUtils.dp2px(10f),size-tableWidth-SizeUtils.dp2px(10f),size-tableWidth-SizeUtils.dp2px(10f))
        mCircleWidth = size/2-tableWidth - SizeUtils.dp2px(10f)
        mPath.reset()
        //在刻度路径中增加一个从起始弧度
        mPath.addArc(mTableRectF, 50f, 260f)
        //计算路径的长度
        val pathMeasure = PathMeasure(mPath, false)
        val length = pathMeasure.length
        val step = length / 60
        dashPathEffect = DashPathEffect(floatArrayOf(step / 3, step * 2 / 3), 10f)
        val radius = size / 2
        mColorShader = SweepGradient(radius, radius, SWEEP_GRADIENT_COLORS, null)
        mProgressColorShader = SweepGradient(radius,radius,PROGRESS_COLORS, floatArrayOf(0.5f,1f))
        mProgressPaint.shader = mProgressColorShader
        //设置画笔
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = tableWidth.toFloat()
        mPaint.pathEffect = dashPathEffect
        mPaint.shader = mColorShader

        //设置指针的路径位置
        mPointerPath!!.reset()
        mPointerPath!!.moveTo(radius, radius - 20)
        mPointerPath!!.lineTo(radius, radius + 20)
        mPointerPath!!.lineTo(radius * 2 - tableWidth, radius)
        mPointerPath!!.close()
    }
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val dx = (width - mTableRectF.width()) / 2
        val dy = (height - mTableRectF.height()) / 2

        //把刻度的方框平移到正中间
        canvas!!.translate(dx, dy)
        canvas.save()
        //旋转画布
        canvas.rotate(90f, mTableRectF.width() / 2, mTableRectF.height() / 2)
        canvas.drawPath(mPath, mPaint)
        canvas.restore()




        when(connectState){

            0 ->{  //未连接
                //画圆环
                textPaint.getTextBounds(noConnect,0,noConnect.length,textRect)// 获取文字高度
                canvas.drawCircle(mTableRectF.width()/2,mTableRectF.height()/2,mCircleWidth,mBgPaint)
                canvas.drawText(noConnect,mTableRectF.width()/2,
                    mTableRectF.height()/2+textRect.height()/2,textPaint
                )
            }
            1 ->{ //连接中
                textPaint.getTextBounds(connecting,0,connecting.length,textRect)// 获取文字高度
                canvas.drawText(connecting,mTableRectF.width()/2,
                    mTableRectF.height()/2+textRect.height()/2,textPaint
                )
                //画进度条
                canvas.rotate(mProgressAngle,mTableRectF.width()/2,mTableRectF.height()/2)
                canvas.save()
                canvas.drawCircle(mTableRectF.width()/2,mTableRectF.height()/2,mCircleWidth,mProgressPaint)
                canvas.drawCircle(mTableRectF.width()-tableWidth-SizeUtils.dp2px(10f),mTableRectF.height()/2,tableWidth.toFloat()/2,mCirclePaint)
                canvas.drawCircle(mTableRectF.width()-tableWidth-SizeUtils.dp2px(10f),mTableRectF.height()/2,SizeUtils.dp2px(3f).toFloat(),textPaint)
                canvas.restore()
            }
            2 ->{ //连接成功
                mBgPaint.setColor(Color.parseColor("#4080FD"))
                textPaint.getTextBounds(noConnect,0,noConnect.length,textRect)// 获取文字高度
                canvas.drawText("Free time",mTableRectF.width()/2,mTableRectF.height()/2-textRect.height()/2,textPaint)
                canvas.drawText("13:45",mTableRectF.width()/2,mTableRectF.height()/2+textRect.height()+textRect.height()/2,textPaint)
                canvas.drawCircle(mTableRectF.width()/2,mTableRectF.height()/2,mCircleWidth,mBgPaint)
            }
        }
    }


    fun start(){
        isConnecting = true
        connectState=1
        mHander.post { mRunnable.run() }
    }
    fun stop(state:Boolean){
        isConnecting = false
        connectState = if(state) 2 else 0
        postInvalidate()
        mHander.removeCallbacks(mRunnable)
    }
}