/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.funbizmodel.bookstore;

import org.funbizmodel.bookstore.model.author.AuthorQuerier;
import org.funbizmodel.bookstore.model.author.AuthorService;
import org.funbizmodel.bookstore.model.book.BookQuerier;
import org.funbizmodel.bookstore.model.book.BookService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.funbizmodel.bookstore.model.author.AuthorService.DELETE;
import static org.funbizmodel.bookstore.model.author.AuthorService.update;

/**
 * @author Carlos Sierra Andrés
 */
public class Main {

	public static void main(String[] args)
		throws ClassNotFoundException, SQLException {

		Class.forName("org.h2.Driver");

		try (Connection conn = DriverManager.getConnection(
			"jdbc:h2:./bookstore", "sa", "")) {

			conn.prepareStatement(
				"DROP TABLE AUTHOR IF EXISTS; " +
				"DROP TABLE BOOK IF EXISTS; " +
				"DROP TABLE AUTHOR_BOOK IF EXISTS;").
			executeUpdate();

			conn.prepareStatement(
				"CREATE TABLE AUTHOR(id long primary key auto_increment, name varchar);" +
				"CREATE TABLE BOOK(id long primary key auto_increment, isbn varchar, title varchar);" +
				"CREATE TABLE AUTHOR_BOOK (authorId long, bookId long);").
			executeUpdate();

			AuthorService author = new AuthorService(conn);

			BookService books = new BookService(conn);

			author.setBookService(books);
			books.setAuthorService(author);

			String zutanoId = author.create(ab -> ab.name("Zutano")).map(AuthorQuerier::id).get();


			//Create author and books... one book has one extra author
			author.create(ab -> ab.
					name("Federico").
					books(books.create(
							bb -> bb.isbn("oneisbn").title("onetitle").addAuthors(Stream.of(author.withId(zutanoId))),
							bb -> bb.isbn("anotherisbn").title("anothertitle")
						)
					)
			).map(AuthorQuerier::id);


			//Get the authors of each of these two books
			books.fromTitles("onetitle", "anothertitle").forEach(bc ->
					author.fromBook(bc).forEach(
						ac ->
							System.out.println(ac.map(AuthorQuerier::name))
					)
			);

			//Create a new book and assign it a new author
			books.create(
				bb -> bb.isbn("thirdisbn").title("yetanothertitle").
					addAuthors(Stream.of(author.create(ab -> ab.name("Fulano"))))).
				map(BookQuerier::id);

			//Create a view object from the query before
			author.withId(zutanoId).map(
				aq -> new AuthorWithBooks(
					aq.name(), aq.books(BookQuerier::title).collect(Collectors.toList())
				)
			).andThen(System.out::println);

			//Update author changing his name and adding a book. Query the
			// resulting books of that author

			author.withId(zutanoId).execute(
				update(au -> {
					au.setNewName("Mengano");
					au.addBooks(books.fromTitles("yetanothertitle"));
				})).
				map(aq -> aq.books(BookQuerier::title)).andThen(s -> s.forEach(System.out::println));

			//Create a view object from the query after
			author.withId(zutanoId).map(
				aq -> new AuthorWithBooks(
					aq.name(), aq.books(BookQuerier::title).collect(Collectors.toList())
				)
			).andThen(System.out::println);

			//DELETE the author
			author.withId(zutanoId).execute(DELETE);

			//IT does not exist anymore
			author.withId(zutanoId).map(AuthorQuerier::name).orElse(c -> System.out.println("DOES NOT EXIST!!"));

			//Other authors still exist
			books.all().map(
				bc -> bc.map(
					bq -> bq.authors(AuthorQuerier::name))).
				forEach(
					r -> System.out.println(
						r.getOrElse(
							e -> Stream.of("ERROR!")).collect(Collectors.toList())
					)
				);

		}
	}

	static class AuthorWithBooks {
		String name;

		List<String> bookTitles;

		public AuthorWithBooks(String name, List<String> bookTitles) {
			this.name = name;
			this.bookTitles = bookTitles;
		}

		@Override
		public String toString() {
			return "AuthorWithBooks{" +
				"name='" + name + '\'' +
				", bookTitles=" + bookTitles +
				'}';
		}
	}
}