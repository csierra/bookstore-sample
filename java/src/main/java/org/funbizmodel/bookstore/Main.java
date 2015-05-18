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

import org.funbizmodel.bookstore.model.author.AuthorContext;
import org.funbizmodel.bookstore.model.author.AuthorQuerier;
import org.funbizmodel.bookstore.model.author.AuthorService;
import org.funbizmodel.bookstore.model.book.BookQuerier;
import org.funbizmodel.bookstore.model.book.BookService;
import org.funbizmodel.bookstore.service.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Stream;

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
				"DROP TABLE AUTHOR IF EXISTS; DROP TABLE BOOK IF EXISTS; DROP TABLE AUTHOR_BOOK IF EXISTS;").executeUpdate();

			conn.prepareStatement(
				"CREATE TABLE AUTHOR(id long primary key auto_increment, name varchar);" +
				"CREATE TABLE BOOK(id long primary key auto_increment, isbn varchar, title varchar);" +
				"CREATE TABLE AUTHOR_BOOK (authorId long, bookId long);").executeUpdate();

			AuthorService author = new AuthorService(conn);

			BookService books = new BookService(conn);

			author.setBookService(books);
			books.setAuthorService(author);

			AuthorContext zutano = author.create(ab -> ab.name("Zutano"));

			String zutanoId = zutano.andMap(AuthorQuerier::id).get();


			//Create author and books... one book has one extra author
			author.create(ab -> ab.
					name("Federico").
					books(books.create(
							bb -> bb.isbn("oneisbn").title("onetitle").addAuthors(Stream.of(zutano)),
							bb -> bb.isbn("anotherisbn").title("anothertitle")
						)
					)
			).andMap(AuthorQuerier::id);


			//Get the authors of each of these two books
			books.fromTitles("onetitle", "anothertitle").forEach(bc ->
					author.fromBook(bc).forEach(
						ac ->
							System.out.println(ac.andMap(AuthorQuerier::name))
					)
			);

			//Create a new book and assign it a new author
			books.create(
				bb -> bb.isbn("thirdisbn").title("yetanothertitle").
					addAuthors(Stream.of(author.create(ab -> ab.name("Fulano"))))).
			andMap(BookQuerier::id);

			//Update author changing his name and adding a book. Query the
			// resulting books of that author
			Result<Stream<Result<String>>> updatedBooks =
				author.withId(zutanoId).execute(
					update(au -> {
						au.setNewName("Mengano");
						au.addBooks(books.fromTitles("yetanothertitle"));
					})).
					andMap(aq -> aq.books().map(
						bq -> bq.andMap(BookQuerier::title)));

			updatedBooks.get().forEach(System.out::println);
		}
	}

}
