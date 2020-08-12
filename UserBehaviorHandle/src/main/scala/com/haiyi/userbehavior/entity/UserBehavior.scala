package com.haiyi.userbehavior.entity

/**
 * @author Mr.Xu
 * @create 2020-08-11
 *
 */
case class UserBehavior(
                       userId:Long,
                       itemId:Long,
                       categoryId:Int,
                       behavior:String,
                       timestamp:Long
                       )
