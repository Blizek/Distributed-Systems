package com.example;

import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

public class Server {
    public static void main(String[] args) throws Exception {
        io.grpc.Server server = ServerBuilder.forPort(50051)
                .addService(new LibraryServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("Server started on port 50051");
        server.start();
        server.awaitTermination();
    }
}