package com.example.news

import com.example.Util
import spock.lang.Specification

/**
 * @author zzhao
 */
class UtilSpec extends Specification {

    def 'byte to unsigned int'() {
        expect: 'correct conversion between unsigned byte and int'
        Util.unsignedToInt(0 as byte) == 0 as int
        Util.unsignedToInt(1 as byte) == 1 as int
        Util.unsignedToInt(-1 as byte) == 255 as int
        Util.unsignedToInt(255 as byte) == 255 as int
        Util.unsignedToInt(127 as byte) == 127 as int
        Util.unsignedToInt(130 as byte) == 130 as int
    }
}
