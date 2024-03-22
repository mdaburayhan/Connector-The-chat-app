package com.arsoftltd.connector.models;

import java.io.Serializable;

public class User implements Serializable {
    public String name, image, email, token, id;

    public String getName() {
        return name;
    }
}
