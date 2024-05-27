package com.example.playbilld;

import android.content.res.Resources;

import androidx.fragment.app.Fragment;


/** Ebben az osztályban számon lehet tartani egy hónapszámot és egy évszámot,
 * illetve le lehet kérni az adott hónap nevét **/
public class MonthYear {

    private int month;

    private int year;

    static String getMonthName(Fragment fragment, int month_number) {

        Resources res = fragment.getResources();
        if (month_number > 0 && month_number <= 12) {
            String[] month_names = res.getStringArray(R.array.months);
            return month_names[month_number - 1];
        } else {
            return "Error";
        }
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
