package com.bass.easySerial.wrapper

import com.bass.easySerial.EasySerialBuilder
import com.bass.easySerial.SerialPort

/**
 * Create by BASS
 * on 2021/12/23 17:47.
 * 串口通信的基类
 */
@Suppress("unused", "SpellCheckingInspection")
abstract class BaseEasySerialPort internal constructor() {

    protected var serialPort: SerialPort? = null//串口通信类
    protected var customBufferSize = 64//串口单次接收数据的最大字节数
    protected var readInterval = 10L//串口数据读取的间隔 单位为毫秒

    internal fun initSerialPort(serialPort: SerialPort) {
        this.serialPort = serialPort
    }

    /**
     * 获取串口的名称
     * 如：/dev/ttyS4
     */
    fun getPortPath() = serialPort?.getDevicePath()

    /**
     * 强转成 [EasyKeepReceivePort]
     * @exception ClassCastException 如果类型不匹配,则抛出异常
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    fun <CallBackType> cast2KeepReceivePort(): EasyKeepReceivePort<CallBackType> {
        return this as EasyKeepReceivePort<CallBackType>
    }

    /**
     * 强转成[EasyWaitRspPort]
     * @exception ClassCastException 如果类型不匹配,则抛出异常
     */
    @Throws(ClassCastException::class)
    fun cast2WaitRspPort(): EasyWaitRspPort {
        return this as EasyWaitRspPort
    }

    /**
     * 调用此方法将关闭串口
     */
    open suspend fun close() {
        //关闭串口
        serialPort?.closeSerial()
        //移除串口类实例,下次才可再创建
        EasySerialBuilder.remove(this)
    }

}