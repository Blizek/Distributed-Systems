package com.example;

import com.example.library.LibraryProto.*;
import com.example.library.LibraryServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryServiceImpl extends LibraryServiceGrpc.LibraryServiceImplBase {

    private static final List<Book> BOOKS = Arrays.asList(
            Book.newBuilder()
                    .setId("1").setTitle("Quo vAIdis").setAuthor("Andrzej Dragan")
                    .addTags("programming").addTags("ai").setPrice(49.99)
                    .build(),
            Book.newBuilder()
                    .setId("2").setTitle("1984").setAuthor("George Orwell")
                    .addTags("sci-fi").addTags("bestseller").setPrice(39.99)
                    .build(),
            Book.newBuilder()
                    .setId("3").setTitle("Langer").setAuthor("Remigiusz Mróz")
                    .addTags("criminal").addTags("bestseller").addTags("for-adults").setPrice(69.99)
                    .build(),
            Book.newBuilder()
                    .setId("4").setTitle("LLM explained").setAuthor("Demis Hassabis")
                    .addTags("programming").addTags("ai").addTags("llm").setPrice(54.99)
                    .build()
    );

    @Override
    public void getBook(BookRequest request, StreamObserver<Book> responseObserver) {
        System.out.println("[Server] GetBook id=" + request.getId());
        Book found = null;
        for (Book b : BOOKS) {
            if (b.getId().equals(request.getId())) {
                found = b;
                break;
            }
        }

        if (found != null) {
            responseObserver.onNext(found);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Book not found: " + request.getId())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void listBooks(Empty request, StreamObserver<Book> responseObserver) {
        System.out.println("[Server] ListBooks — streaming " + BOOKS.size() + " books");
        for (Book book : BOOKS) {
            responseObserver.onNext(book);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void searchBooks(SearchRequest request, StreamObserver<BooksResponse> responseObserver) {
        System.out.println("[Server] SearchBooks query=" + request.getQuery());
        String q = request.getQuery().toLowerCase();
        int limit = request.getMaxResults() > 0 ? request.getMaxResults() : Integer.MAX_VALUE;

        List<Book> results = new ArrayList<>();
        for (Book b : BOOKS) {
            if (results.size() >= limit) break;

            boolean matches = b.getTitle().toLowerCase().contains(q)
                    || b.getAuthor().toLowerCase().contains(q);

            if (!matches) {
                for (String tag : b.getTagsList()) {
                    if (tag.contains(q)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                results.add(b);
            }
        }

        responseObserver.onNext(
                BooksResponse.newBuilder()
                        .addAllBooks(results)
                        .setTotalCount(results.size())
                        .build()
        );
        responseObserver.onCompleted();
    }
}