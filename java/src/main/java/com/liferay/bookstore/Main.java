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

package com.liferay.bookstore;

import com.liferay.bookstore.model.author.AuthorQuerier;
import com.liferay.bookstore.model.author.AuthorService;
import com.liferay.bookstore.model.book.BookService;
import com.liferay.bookstore.service.CorrectResult;
import com.liferay.bookstore.service.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * @author Carlos Sierra Andrés
 */
public class Main {

	public static void main(String[] args)
		throws ClassNotFoundException, SQLException {

		Class.forName("org.h2.Driver");

		try (Connection conn = DriverManager.getConnection(
			"jdbc:h2:./bookstore", "sa", "")) {

			AuthorService author = new AuthorService(conn);

			BookService books = new BookService(conn);

			author.setBookService(books);
			books.setAuthorService(author);

			Result<String> result = author
				.create(a -> a
					.name("A new User")
					.books(currentAuthor ->
							books.create(
								b -> b
									.title("A new book")
									.isbn("ISBN1")
									.setAuthor(currentAuthor),
								b -> b
									.title("Another new book")
									.isbn("ISBN2")
									.setAuthor(currentAuthor))
					))
				.andMap(AuthorQuerier::id);

			System.out.println(result);
		}
	}

}
