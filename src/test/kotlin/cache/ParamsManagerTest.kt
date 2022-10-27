package cache

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ParamsManagerTest {
    @Test
    fun testParseRevision209() {
        val paramsManager = ParamsManager()
        val params = """
            Created at Wed Oct 19 19:59:34 CDT 2022

            codebase=http://oldschool194.runescape.com/
            mainclass=client.class

            param=2=https://payments.jagex.com/
            param=3=true
            param=4=1
            param=5=1
            param=6=0
            param=7=0
            param=8=true
            param=9=ElZAIrq5NpKN6D3mDdihco3oPeYN2KFy2DCquj7JMmECPmLrDP3Bnw
            param=10=5
            param=11=https://auth.jagex.com/
            param=12=494
            param=13=.runescape.com
            param=14=0
            param=15=0
            param=16=false
            param=17=http://www.runescape.com/g=oldscape/slr.ws?order=LPWM
            param=18=
            param=19=196515767263-1oo20deqm6edn7ujlihl6rpadk9drhva.apps.googleusercontent.com
            param=20=https://social.auth.jagex.com/
            param=21=0
            param=25=209
            param=28=https://account.jagex.com/
        """

        val inputStream = ByteArrayInputStream(params.toByteArray())
        paramsManager.parseParams(inputStream.bufferedReader())

        val expectedRevisionNumber = 209
        assertEquals(expectedRevisionNumber, paramsManager.getParam(ParamType.REVISION)?.toInt())
    }

    @Test
    fun testParseParams2018() {
        val paramsManager = ParamsManager()
        val params = """
            Created at Thu Feb 15 13:58:59 CST 2018

            codebase=http://oldschool21.runescape.com/
            mainclass=client.class
            
            param=1=1
            param=2=.runescape.com
            param=3=
            param=4=ElZAIrq5NpKN6D3mDdihco3oPeYN2KFy2DCquj7JMmECPmLrDP3Bnw
            param=5=0
            param=6=true
            param=7=5
            param=8=2521
            param=9=321
            param=10=0
            param=11=0
            param=12=1
            param=13=true
            param=14=0
            param=15=http://www.runescape.com/g=oldscape/slr.ws?order=LPWM
            param=16=false
        """

        val inputStream = ByteArrayInputStream(params.toByteArray())
        paramsManager.parseParams(inputStream.bufferedReader())

        val expectedWebUrl = ".runescape.com"
        assertEquals(expectedWebUrl, paramsManager.getParam(ParamType.WEB_URL))
    }
}
