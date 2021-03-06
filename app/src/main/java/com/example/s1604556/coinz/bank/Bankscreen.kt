package com.example.s1604556.coinz.bank

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.example.s1604556.coinz.R
import kotlinx.android.synthetic.main.bank_activity.*

class Bankscreen : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bank_activity)
        //display current gold
        displayGold.text = BankObject.bank.gold.toString()
    }

    //the following buttons will start activity for the corresponding coin page
    fun bankdollar(view: View){
        view.isClickable=false
        Handler().postDelayed({
            view.isClickable=true
        },2000)

        val intent = Intent(this, BankDollarPage::class.java)
        startActivity(intent)
    }

    fun bankpeny(view: View){
        view.isClickable=false
        Handler().postDelayed({
            view.isClickable=true
        },2000)
        val intent = Intent(this, BankPennyPage::class.java)
        startActivity(intent)
    }

    fun bankshil(view: View){
        view.isClickable=false
        Handler().postDelayed({
            view.isClickable=true
        },2000)
        val intent = Intent(this, BankShilPage::class.java)
        startActivity(intent)
    }

    fun bankquid(view: View){
        view.isClickable=false
        Handler().postDelayed({
            view.isClickable=true
        },2000)
        val intent = Intent(this, BankQuidPage::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        //when returning from other page(coins pages in bank), update the gold displayed
        displayGold.text = BankObject.bank.gold.toString()

    }



}