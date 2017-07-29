package me.zhenhao.forced.decisionmaker

import java.util.Random


object DeterministicRandom {

    var theRandom = Random(23)

    fun reinitialize(seed: Int) {
        theRandom = Random(seed.toLong())
    }

}
