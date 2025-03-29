package org.example;

import lombok.Data;

@Data
public class Server {
    private int id;
    private int vCPUs;
    private int ram;
    private int disk;
}
