package test;

/*
 * To execute Java, please define "static void main" on a class named Solution.
 *
 * If you need more classes, simply define them inline.
 */

public class Solution {

    public static void main(String[] args) {

    }

    static class Customer {
        private String lastName;
        private String firstName;
        private Integer age;

        public Customer() {}

        public Customer(String firstName, String lastName, Integer age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public Integer getAge() {
            return age;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        @Override
        public String toString() {
            return firstName + " " + lastName + " " + age;
        }
    }

}


