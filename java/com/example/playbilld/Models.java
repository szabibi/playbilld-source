package com.example.playbilld;

import android.bluetooth.BluetoothProfile;
import android.os.Parcelable;

import java.io.Serializable;

/** Az adatbázisban szereplő "táblák" modelljei **/
public class Models {

    static public class User {
        private String email;
        private boolean google;

        private String fcmToken;

        User() {}

        public User(String email, boolean google) {
            this.email = email;
            this.google = google;
        }

        public User(String email, boolean google, String fcmToken) {
            this.email = email;
            this.google = google;
            this.fcmToken = fcmToken;
        }

        public String getEmail() {
            return email;
        }

        public boolean isGoogle() {
            return google;
        }

        public String toString() {
            return "email: " + email + ", google: " + google;
        }

        public String getFcmToken() {
            return fcmToken;
        }

        public void setFcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
        }
    }

    static public class Show implements Comparable<Show> {

        private int id;
        private String title;
        private int premiere;
        private String music_by;
        private String imgsrc;

        private float average;

        private int ratings;

        Show() {}

        public void setAverage(float average) {
            this.average = average;
        }

        public void incrementRatings() {
            this.ratings += 1;
        }

        public void decrementRatings() {
            this.ratings -= 1;
        }

        public float getAverage() {
            return average;
        }

        public int getRatings() {
            return ratings;
        }

        public void setRatings(int ratings) {
            this.ratings = ratings;
        }

        public Show(String title, int premiere, String music_by, String imgsrc, float average, int ratings) {
            this.title = title;
            this.premiere = premiere;
            this.music_by = music_by;
            this.imgsrc = imgsrc;
            this.average = average;
            this.ratings = ratings;
        }

        public String getTitle() {
            return title;
        }

        public String getImgsrc() {
            return imgsrc;
        }

        public int getPremiere() {
            return premiere;
        }
        public String getPremiereString() {
            return Integer.toString(premiere);
        }

        public String getMusic_by() {
            return music_by;
        }

        @Override
        public int compareTo(Show other) {
            int compareTitle = this.title.compareTo(other.getTitle());
            if (compareTitle != 0) {
                return compareTitle;
            }
            else return this.premiere - other.getPremiere();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String toString() {
            return "id: " + id + ", title: " + title + ", premiere: " + premiere + ", music_by: " + music_by;
        }
    }

    static public class Log implements Comparable<Log>, Serializable {
        public void setDate(String date) {
            this.date = date;
        }

        public void setLiked(boolean liked) {
            this.liked = liked;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public void setRating(double rating) {
            this.rating = rating;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        private String date;
        private int year;
        private int month;
        private int day;
        private String user;

        private boolean liked;
        private String review;
        private double rating;
        private String location;
        private String show;
        private String showImgSrc;
        private String showTitle;

        private String id;

        Log() {}

        @Override
        public String toString() {
            return "Log{" +
                    "date='" + date + '\'' +
                    ", user='" + user + '\'' +
                    ", liked=" + liked +
                    ", review='" + review + '\'' +
                    ", rating=" + rating +
                    ", location='" + location + '\'' +
                    ", show='" + show + '\'' +
                    ", showImgSrc='" + showImgSrc + '\'' +
                    ", showTitle='" + showTitle + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }

        public Log(String date, boolean liked, String location, double rating, String review, String show, String showImgSrc, String showTitle) {
            this.date = date;
            this.liked = liked;
            this.review = review;
            this.rating = rating;
            this.location = location;
            this.show = show;
            this.showImgSrc = showImgSrc;
            this.showTitle = showTitle;
        }

        public Log buildRest(String username) {
            user = username;

            String[] dateParts = date.split("-");
            android.util.Log.d("dateParts", dateParts.toString());
            year = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(dateParts[2]);

            return this;
        }

        @Override
        public int compareTo(Log o) {
            int compareYear = year - o.getYear();
            if (compareYear != 0) {
                return compareYear;
            } else {
                int compareMonth = month - o.getMonth();
                if (compareMonth != 0) {
                    return compareMonth;
                } else {
                    int compareDay = day - o.getDay();
                    if (compareDay != 0) {
                        return compareDay;
                    } else {
                        return -showTitle.compareTo(o.getShowTitle());
                    }
                }
            }
        }

        public String getDate() {
            return date;
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getDay() {
            return day;
        }

        public String getUser() {
            return user;
        }

        public String getReview() {
            return review;
        }

        public double getRating() {
            return rating;
        }

        public String getLocation() {
            return location;
        }

        public String getShow() {
            return show;
        }

        public boolean isLiked() {
            return liked;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getShowTitle() {
            return showTitle;
        }

        public void setShowTitle(String showTitle) {
            this.showTitle = showTitle;
        }

        public String getShowImgSrc() {
            return showImgSrc;
        }

        public void setShowImgSrc(String showImgSrc) {
            this.showImgSrc = showImgSrc;
        }

    }
}
