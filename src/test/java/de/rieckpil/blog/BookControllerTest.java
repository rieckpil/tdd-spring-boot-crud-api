package de.rieckpil.blog;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
public class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private BookService bookService;

  @Captor
  private ArgumentCaptor<BookRequest> bookRequestArgumentCaptor;

  @Test
  public void postingANewBookShouldCreateANewBook() throws Exception {

    when(bookService.createNewBook(bookRequestArgumentCaptor.capture())).thenReturn(1L);

    this.mockMvc
      .perform(post("/api/books")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
           "author": "Duke",
           "isbn": "1337",
           "title": "Java 11"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(header().string("Location", "http://localhost/api/books/1"));

    assertThat(bookRequestArgumentCaptor.getValue().getAuthor(), is("Duke"));
    assertThat(bookRequestArgumentCaptor.getValue().getIsbn(), is("1337"));
    assertThat(bookRequestArgumentCaptor.getValue().getTitle(), is("Java 11"));

  }

  @Test
  public void allBooksEndpointShouldReturnTwoBooks() throws Exception {

    when(bookService.getAllBooks()).thenReturn(List.of(
      createBook(1L, "Java 11", "Duke", "1337"),
      createBook(2L, "Java EE 8", "Duke", "1338")));

    this.mockMvc
      .perform(get("/api/books"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$", hasSize(2)))
      .andExpect(jsonPath("$[0].title", is("Java 11")))
      .andExpect(jsonPath("$[0].author", is("Duke")))
      .andExpect(jsonPath("$[0].isbn", is("1337")))
      .andExpect(jsonPath("$[0].id", is(1)));

  }

  @Test
  public void getBookWithIdOneShouldReturnABook() throws Exception {

    when(bookService.getBookById(1L)).thenReturn(createBook(1L, "Java 11", "Duke", "1337"));

    this.mockMvc
      .perform(get("/api/books/1"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.title", is("Java 11")))
      .andExpect(jsonPath("$.author", is("Duke")))
      .andExpect(jsonPath("$.isbn", is("1337")))
      .andExpect(jsonPath("$.id", is(1)));

  }

  @Test
  public void getBookWithUnknownIdShouldReturn404() throws Exception {

    when(bookService.getBookById(1L)).thenThrow(new BookNotFoundException("Book with id '1' not found"));

    this.mockMvc
      .perform(get("/api/books/1"))
      .andExpect(status().isNotFound());

  }

  @Test
  public void updateBookWithKnownIdShouldUpdateTheBook() throws Exception {

    when(bookService.updateBook(eq(1L), bookRequestArgumentCaptor.capture()))
      .thenReturn(createBook(1L, "Java 15", "Duke", "1337"));

    this.mockMvc
      .perform(put("/api/books/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
           "author": "Duke",
           "isbn": "1337",
           "title": "Java 15"
          }
          """))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.title", is("Java 15")))
      .andExpect(jsonPath("$.author", is("Duke")))
      .andExpect(jsonPath("$.isbn", is("1337")))
      .andExpect(jsonPath("$.id", is(1)));

    assertThat(bookRequestArgumentCaptor.getValue().getAuthor(), is("Duke"));
    assertThat(bookRequestArgumentCaptor.getValue().getIsbn(), is("1337"));
    assertThat(bookRequestArgumentCaptor.getValue().getTitle(), is("Java 15"));

  }

  @Test
  public void updateBookWithUnknownIdShouldReturn404() throws Exception {

    when(bookService.updateBook(eq(42L), bookRequestArgumentCaptor.capture()))
      .thenThrow(new BookNotFoundException("The book with id '42' was not found"));

    this.mockMvc
      .perform(put("/api/books/42")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
            {
             "author": "Duke",
             "isbn": "1337",
             "title": "Java 12"
            }
            """))
      .andExpect(status().isNotFound());

  }

  private Book createBook(Long id, String title, String author, String isbn) {
    Book book = new Book();
    book.setAuthor(author);
    book.setIsbn(isbn);
    book.setTitle(title);
    book.setId(id);
    return book;
  }
}
