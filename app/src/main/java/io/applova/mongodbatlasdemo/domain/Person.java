package io.applova.mongodbatlasdemo.domain;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class Person extends RealmObject {
    @PrimaryKey
    private String _id; // Changed from 'id' to '_id'

    @Required
    private String name;

    private int age;

    public Person() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
