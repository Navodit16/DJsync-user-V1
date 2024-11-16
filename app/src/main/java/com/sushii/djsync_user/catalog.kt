package com.sushii.djsync_user

class Catalog {

    fun songQuantity(points :Int): Int{
        var n = 0
        when(points){
            0 -> n =1
            10 -> n = 2
            20 -> n = 3
            30 -> n = 4
            40 -> n= 5
            else -> n = 6
        }
        return n
    }
}
