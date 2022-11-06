# EasySerial串口通信SDK

------------------------------------------------------------------

#### 此项目移植于谷歌官方串口库[android-serialport-api](https://code.google.com/archive/p/android-serialport-api/) ，以及[GeekBugs-Android-SerialPort](https://github.com/GeekBugs/Android-SerialPort) ，综合了这俩个库的C代码，对其进行封装，以适应串口读取的不同需求；

## 作者有话要说

--------------------------------------------------------------------------
**1. 此SDK本人已经在多个项目中实践使用，如果你在使用中有任何问题，请在issue中向我提出；**  
**2. 当前仅支持Kotlin项目使用，对于java调用做的并不完美；**  
**3. 此SDK可以创建2种不同作用的串口对象，一个是保持串口接收的串口对象，一个是写入并同步等待数据返回的对象；**

--------------------------------------------------------------------------

## SDK的使用介绍

--------------------------------------------------------------------------

### **EasyKeepReceivePort的使用：**

**1. 创建一个永久接收的串口(串口开启失败返回Null)；   
演示：不做自定义返回数据的处理；**

A). 创建一个串口,串口返回的数据默认为ByteArray类型；

```Kotlin
val port = EasySerialBuilder.createKeepReceivePort<ByteArray>("/dev/ttyS4", BaudRate.B4800)
```

我们还可以这样创建串口,串口返回的数据默认为ByteArray类型；

```Kotlin
val port = EasySerialBuilder.createKeepReceivePort<ByteArray>(
            "/dev/ttyS4",
            BaudRate.B4800, DataBit.CS8, StopBit.B1,
            Parity.NONE, 0, FlowCon.NONE
        )
```

B). 设置单次接收数据的最大字节数,默认为64个字节；
```Kotlin
port.setBufferSize(64)
```

C).设置数据的读取间隔；   
即上一次读取完数据后,隔多少秒后读取下一次数据；   
默认为10毫秒,读取时间越短，CPU的占用会越高,请合理配置此设置；
```Kotlin
port.setReadInterval(100)
```

D). 监听串口返回的数据;    
第一种写法；须注意，此回调处于协程之中；
```Kotlin
val dataCallBack = port.addDataCallBack {
            //处理项目逻辑；
            // 此处示范将串口数据转化为16进制字符串；
            val hexString = it.conver2HexString()
            Log.d(tag, "接收到串口数据:$hexString")
        }
port.addDataCallBack(dataCallBack)
```
第二种写法；须注意，此回调处于协程之中；
```Kotlin
val dataCallBack = object : EasyReceiveCallBack<ByteArray> {
            override suspend fun receiveData(data: ByteArray) {
                //处理项目逻辑；
                //此处示范将串口数据转化为16进制字符串；
                val hexString = data.conver2HexString()
                Log.d(tag, "接收到串口数据:$hexString")
            }

        }
port.addDataCallBack(dataCallBack)
```

E). 移除串口监听；
```Kotlin
 port.removeDataCallBack(dataCallBack)
```

F). 关闭串口；   
使用完毕关闭串口，关闭串口须在作用域中关闭，关闭时会阻塞当前协程，直到关闭处理完成；
这个过程并不会耗费太长时间,一般为1ms-4ms;
```Kotlin
CoroutineScope(Dispatchers.IO).launch { port.close() }
```

**2. 创建一个永久接收的串口(串口开启失败返回Null)；   
演示：自定义回调的数据类型，在接收到串口数据后对数据进行一次处理，再将数据返回给串口数据监听者；**

A). 创建一个串口,串口返回的数据类型,我们自定义为可为null的String类型；
```Kotlin
val port = EasySerialBuilder.createKeepReceivePort<String?>("/dev/ttyS4", BaudRate.B4800)
```
我们还可以这样创建串口,串口返回的数据类型,我们自定义为可为null的String类型；
```Kotlin
val port = EasySerialBuilder.createKeepReceivePort<String?>(
            "/dev/ttyS4",
            BaudRate.B4800, DataBit.CS8, StopBit.B1,
            Parity.NONE, 0, FlowCon.NONE
        )
```

B). 设置单次接收数据的最大字节数,默认为64个字节；
```Kotlin
port.setBufferSize(64)
```

C).设置数据的读取间隔；   
即上一次读取完数据后,隔多少秒后读取下一次数据；   
默认为10毫秒,读取时间越短，CPU的占用会越高,请合理配置此设置；
```Kotlin
port.setReadInterval(100)
```

D).因为我们设置数据返回类型不再是默认的ByteArray类型，所以我们需要设置自定义的数据解析规则；
```Kotlin
 port.setDataHandle(CustomEasyPortDataHandle())
```
接下来我们创建一个自定义解析类,并将其命令为CustomEasyPortDataHandle；
```Kotlin
class CustomEasyPortDataHandle : EasyPortDataHandle<String?>() {

    private val stringBuilder = StringBuilder()//用于记录数据
    private val pattern = Pattern.compile("(AT)(.*?)(\r\n)")//用于匹配数据

    /**
     * 数据处理方法
     *
     * @param byteArray 串口收到的原始数据
     * @return 返回自定义处理后的数据,此数据将被派发到各个监听者
     *
     *
     * 我们可以在这里做很多事情，比如有时候串口返回的数据并不是完整的数据，
     * 它可能有分包返回的情况，我们需要自行凑成一个完整的数据后再返回给监听者，
     * 在数据不完整的时候我们直接返回Null给监听者,告知他们这不是一个完整的数据；
     *
     * 在这里我们做个演示,假设数据返回是以AT开头,换行符为结尾的数据是正常的数据；
     *
     */
    override suspend fun portData(byteArray: ByteArray): String? {
        //将串口数据转为16进制字符串
        val hexString = byteArray.conver2HexString()
        //记录本次读取到的串口数据
        stringBuilder.append(hexString)
        //寻找记录中符合规则的数据
        val matcher = pattern.matcher(stringBuilder)
        //没有寻找到符合规则的数据,则返回Null
        if (!matcher.find()) return null
        //寻找到符合规则的数据,将其从记录中删除,并返回数据
        val group = matcher.group()
        stringBuilder.delete(matcher.start(), matcher.end())
        return group
    }

    /**
     * 串口关闭时会回调此方法
     * 如果您需要,可重写此方法,在此方法中做释放资源的操作
     */
    override fun close() {
        stringBuilder.clear()
    }
}
```

E). 监听串口返回的数据；   
此时，我们监听到的数据为我们一开始设置的String?类型；
```kotlin
  val dataCallBack = object : EasyReceiveCallBack<String?> {
            override suspend fun receiveData(data: String?) {
                //为Null是我们自定义的不完整的数据的情况,我们这里不处理不完整的数据；
                data ?: return
                //处理项目逻辑；
                //此处演示直接将转化后的数据类型打印出来；
                Log.d(tag, "接收到串口数据:$data")
            }

        }
```

F). 移除串口监听，关闭串口与之前的一致;
```kotlin
port.addDataCallBack(dataCallBack)//添加串口数据监听
port.removeDataCallBack(dataCallBack)//移除串口数据监听
CoroutineScope(Dispatchers.IO).launch { port.close() }//关闭串口
```

--------------------------------------------------------------------------

### **EasyWaitRspPort的使用：**

A). 创建一个发送后再接收的串口(串口开启失败返回Null)；
```kotlin
val port = EasySerialBuilder.createWaitRspPort("/dev/ttyS4", BaudRate.B4800)
```
我们还可以这样创建串口:
```kotlin
val port = EasySerialBuilder.createWaitRspPort(
            "/dev/ttyS4",
            BaudRate.B4800, DataBit.CS8, StopBit.B1,
            Parity.NONE, 0, FlowCon.NONE
        )
```

B). 设置单次接收数据的最大字节数,默认为64个字节；
```Kotlin
port.setBufferSize(64)
```

C).设置数据的读取间隔；   
即上一次读取完数据后,隔多少秒后读取下一次数据；   
默认为10毫秒,读取时间越短，CPU的占用会越高,请合理配置此设置；
```Kotlin
port.setReadInterval(100)
```

D). 接下来演示发送串口命令的3种方法

**1. 调用写入函数，并阻塞等待200ms，阻塞完成之后将会返回此期间接收到的串口数据；**   
需要注意的是,此方法需要在协程作用域中调用；
```kotlin
val rspBean : WaitResponseBean = port.writeWaitRsp(orderByteArray1)
```
此外，我们也可以在调用此函数时指定等待时长，此处我们演示等待500ms：
```kotlin
val rspBean : WaitResponseBean = port.writeWaitRsp(orderByteArray1, 500)
```
```rspBean```是一个封装的数据类,我们来讲解一下：
```kotlin
rspBean.bytes
```
这是串口返回的数据,此字节数组的大小为我们```setBufferSize()```时输入的字节大小；需要注意的是,字节数组内的字节并不全是串口返回的数据；   

默认的字节数组大小为64，我们假设串口返回了4个字节的数据，那么其余的60个字节都是0；   

那我们怎么知道实际收到了多少个字节呢？这就需要用到数据类内的另一个数据：
```kotlin
rspBean.size
```
这是串口实际读取的字节长度，所以我们取串口返回的实际字节数组可以这样取：
```kotlin
val portBytes = rspBean.bytes.copyOf(rspBean.size)
```
插句题外话，我们也提供了直接将读取到的字节转为16进制字符串的方法:
```kotlin
 val hexString = rspBean.bytes.conver2HexString(rspBean.size)
```

**2. 有时候，我们可能需要连续向串口输出命令，并等待其返回,对此我们也提供了便捷的方案：**   
```kotlin
val rspBeanList : MutableList<WaitResponseBean> = port.writeAllWaitRsp(200, orderByteArray1, orderByteArray2, orderByteArray3)
```
同样的，此方法也是需要在协程作用域中调用；   

我们来讲解一下函数参数：   

第1个参数：单个写入命令的阻塞等待时长，注意，这是单个的，并非所有命令的总阻塞时长；   

第2个参数：一个数组类型，即我们想写入并等待返回的所有指令集；   

```rspBeanList```为多个```WaitResponseBean```的集合，我们在上面已经讲解了```WaitResponseBean```，此处不再赘述；集合中的数据与请求是一一对应：
```kotlin
val rspBean1 : WaitResponseBean = rspBeanList[0]//orderByteArray1
val rspBean2 : WaitResponseBean = rspBeanList[1]//orderByteArray2
val rspBean3 : WaitResponseBean = rspBeanList[2]//orderByteArray3
```

**3. 在同一个串口中,我们有些需要等待串口的数据返回,有些是不需要的,在不需要串口数据返回的情况下，我们可以直接调用写入即可:**
```kotlin
port.write(orderByteArray1)
```
相同的，此方法必须在协程作用域中调用；

E). 关闭串口：  
使用完毕关闭串口，关闭串口须在协程作用域中关闭，关闭时会阻塞当前协程，直到关闭处理完成；
这个过程并不会耗费太长时间,一般为1ms-4ms;
```Kotlin
CoroutineScope(Dispatchers.IO).launch { port.close() }
```

--------------------------------------------------------------------------

### **其他API的使用介绍**

**1. 获取串口对象：**   
每一个串口只会创建一个实例，我们在内部缓存了串口实例，即一处创建,到处可取；如果此串口还未创建，则将获取到Null；
```kotlin
val serialPort: BaseEasySerialPort? = EasySerialBuilder.get("dev/ttyS4")
```
获取到实例后,我们仅可以调用close()方法关闭串口；   
此方法必须在协程作用域中调用；
```kotlin
CoroutineScope(Dispatchers.IO).launch { serialPort.close() }
```
如果你明确知道当前串口属于哪种类型,那么你可以进行类型强转后使用更多特性。如:
```kotlin
val easyWaitRspPort = serialPort.cast2WaitRspPort()
CoroutineScope(Dispatchers.IO).launch { 
    val rspBean = easyWaitRspPort.writeWaitRsp("00 FF AA".conver2ByteArray())
}
```
或者是：
```kotlin
val keepReceivePort = serialPort.cast2KeepReceivePort<ByteArray>()
keepReceivePort.write("00 FF AA".conver2ByteArray())
```

**2. 设置串口不读取字节数：**   
如果你发现，串口无法收到数据，但是可正常写入数据，使用串口调试工具可正常收发，那么你应当试试如下将串口设置为无法读取字节数：
```kotlin
EasySerialBuilder.addNoAvailableDevicePath("dev/ttyS4")
```
设置完后再开启串口,否则设置不生效；也可以直接这么写：
```kotlin
EasySerialBuilder.addNoAvailableDevicePath("dev/ttyS4")
    .createWaitRspPort("dev/ttyS4", BaudRate.B4800)
```
对于`addNoAvailableDevicePath()`方法，需要讲解一下内部串口数据读取的实现了；   
在读取数据时，会先调用`inputStream.available()` 来判断流中有多少个可读字节，但在部分串口中，即使有数据，
`available()`读取到的依旧是0，这就导致了无法读取到数据的情况；   
当调用`addNoAvailableDevicePath()`后， 我们将不再判断流中的可读字节数，而是直接调用`inputStream.read()`方法；
当你使用此方法后，请勿重复开启\关闭串口， 因为这样可能会导致串口无法再工作；

**3. 获取本设备所有的串口名称：**
```kotlin
val allDevicesPath: MutableList<String> = EasySerialFinderUtil.getAllDevicesPath()
allDevicesPath.forEach { Log.d(tag, "串口名称: $it") }
```
**4. 判断当前是否有串口正在使用：**
```kotlin
val hasPortWorking: Boolean = EasySerialBuilder.hasPortWorking()
```

**5. 数据转化：**   
A). 16进制字符串转为字节数组：
```kotlin
val hexString = "00 FF CA FA"
//将16进制字符串 转为 字节数组
val hexByteArray1 = hexString.conver2ByteArray()
//将16进制字符串从第0位截取到第4位("00 FF") 转为 字节数组
val hexByteArray2 = hexString.conver2ByteArray(4)
//将16进制字符串从第2位截取到第4位(" FF") 转为 字节数组
val hexByteArray3 = hexString.conver2ByteArray(2, 4)
```

B). 字节数组转为16进制字符串：
```kotlin
val byteArray = byteArrayOf(0, -1, 10)// 此字节数组=="00FF0A"
//将字节数组 转为 16进制字符串
val hexStr1 = byteArray.conver2HexString()//结果为:"00FF0A"
//将字节数组取1位 转为 16进制字符串
val hexStr2 = byteArray.conver2HexString(1)//结果为:"00"
//将字节数组取2位 转为 16进制字符串 并设置字母为小写
val hexStr3 = byteArray.conver2HexString(2, false)//结果为:"00ff"
//将字节数组取第2位到第3位 转为 16进制字符串 并设置字母为小写
val hexStr4 = byteArray.conver2HexString(1, 2, false)//结果为:"ff0a"
//将字节数组取第0位 转为 16进制字符串 并设置字母为小写
val hexStr5 = byteArray.conver2HexString(0, 0, false)//结果为:"00"

//将字节数组 转为 16进制字符串 16进制之间用空格分隔
val hexStr6 = byteArray.conver2HexStringWithBlank()//结果为:"00 FF 0A"
//将字节数组取2位 转为 16进制字符串 16进制之间用空格分隔
val hexStr7 = byteArray.conver2HexStringWithBlank(2)//结果为:"00 FF"
//将字节数组取2位 转为 16进制字符串 并设置字母为小写
val hexStr8 = byteArray.conver2HexStringWithBlank(2, false)//结果为:"00 ff"
//将字节数组取第2位到第3位 转为 16进制字符串 并设置字母为小写
val hexStr9 = byteArray.conver2HexStringWithBlank(1, 2, false)//结果为:"ff 0a"
//将字节数组取第2位 转为 16进制字符串 并设置字母为小写
val hexStr10 = byteArray.conver2HexStringWithBlank(1, 1, false)//结果为:"ff"
```

C). 字节数组转为字符数组：
```kotlin
val byteArray2 = byteArrayOf('H'.code.toByte(), 'A'.code.toByte(), 'H'.code.toByte(), 'A'.code.toByte())
//将字节数组 转为 字符数组
val charArray1 = byteArray2.conver2CharArray()//即:"HAHA"
//将字节数组取1位 转为 字符数组
val charArray2 = byteArray2.conver2CharArray(1)//即:"H"
//将字节数组取第2位到第3位 转为 字符数组
val charArray3 = byteArray2.conver2CharArray(2, 3)//即:"HA"
//将字节数组第2位 转为 字符数组
val charArray4 = byteArray2.conver2CharArray(2, 2)//即:"H"
```