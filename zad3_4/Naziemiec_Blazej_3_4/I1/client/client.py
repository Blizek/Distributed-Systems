import grpc
from grpc_reflection.v1alpha.proto_reflection_descriptor_database import (
    ProtoReflectionDescriptorDatabase,
)
from google.protobuf.descriptor_pool import DescriptorPool
from google.protobuf.message_factory import GetMessageClass


def connect(host="localhost:50051"):
    channel = grpc.insecure_channel(host)
    db = ProtoReflectionDescriptorDatabase(channel)
    pool = DescriptorPool(db)
    return channel, pool


def msg_class(pool, full_name):
    return GetMessageClass(pool.FindMessageTypeByName(full_name))


def unary(channel, method_path, request_bytes, response_class):
    rpc = channel.unary_unary(
        method_path,
        request_serializer=lambda x: x,
        response_deserializer=response_class.FromString,
    )
    return rpc(request_bytes, wait_for_ready=True)


def streaming(channel, method_path, request_bytes, response_class):
    rpc = channel.unary_stream(
        method_path,
        request_serializer=lambda x: x,
        response_deserializer=response_class.FromString,
    )
    return rpc(request_bytes, wait_for_ready=True)


def print_book(book):
    print(f"(ID: {book.id}) '{book.title}' — {book.author}")
    print(f"Cena: {book.price:.2f}")
    print(f"Tagi: {list(book.tags)}")


def main():
    channel, pool = connect()
    SVC = "library.LibraryService"

    print("\n1) Unarne GetBook(id='2')")
    Req = msg_class(pool, "library.BookRequest")
    Resp = msg_class(pool, "library.Book")
    book = unary(channel, f"/{SVC}/GetBook", Req(id="2").SerializeToString(), Resp)
    print_book(book)

    print("\n2) Server streaming ListBooks()")
    Empty = msg_class(pool, "library.Empty")
    Book = msg_class(pool, "library.Book")
    for i, b in enumerate(
        streaming(channel, f"/{SVC}/ListBooks", Empty().SerializeToString(), Book)
    ):
        print(f"Odebrano #{i + 1}:")
        print_book(b)

    print("\n3) Lista (sekwencja) SearchBooks(query='programming', max_results=3)")
    SearchReq = msg_class(pool, "library.SearchRequest")
    BooksResp = msg_class(pool, "library.BooksResponse")
    resp = unary(
        channel,
        f"/{SVC}/SearchBooks",
        SearchReq(query="programming", max_results=3).SerializeToString(),
        BooksResp,
    )
    print(f"Znaleziono {resp.total_count} wyników:")
    for b in resp.books:
        print_book(b)

    channel.close()


if __name__ == "__main__":
    main()