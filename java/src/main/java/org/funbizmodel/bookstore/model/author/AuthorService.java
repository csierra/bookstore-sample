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

package org.funbizmodel.bookstore.model.author;

import org.funbizmodel.bookstore.model.book.BookContext;
import org.funbizmodel.bookstore.model.book.BookQuerier;
import org.funbizmodel.bookstore.model.book.BookService;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.Service;
import org.funbizmodel.bookstore.service.SqlCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Carlos Sierra Andrés
 */
public class AuthorService
	implements Service<AuthorBuilder, AuthorQuerier, AuthorContext> {

	BookService bookService;
	Connection conn;
	
	public void setBookService(BookService bookService) {
		this.bookService = bookService;
	}

	public AuthorService(Connection conn) {
		this.conn = conn;
	}

	@Override
	public AuthorContext create(Consumer<AuthorBuilder> consumer) {

		return new AuthorCreationContext(conn, consumer);
	}

	@Override
	public Stream<AuthorContext> create(Consumer<AuthorBuilder>... consumers) {
		return Stream.of(consumers).map(this::create);
	}

	@Override
	public Stream<AuthorContext> create(
		Stream<Consumer<AuthorBuilder>> consumers) {

		return consumers.map(this::create);
	}

	@Override
	public AuthorContext withId(String id) {
		return new OnlyAuthorContext(this, id);
	}

	@Override
	public Stream<AuthorContext> all() {
		return Stream.<AuthorContext>builder().build();
	}

	public static SqlCommand<AuthorQuerier> update(
		Consumer<AuthorUpdater> consumer) {

		AuthorUpdater authorUpdater = new AuthorUpdater();

		consumer.accept(authorUpdater);

		SqlCommand<AuthorQuerier> command = (cc) -> {};

		if (authorUpdater.newName != null) {
			command = command.andThen(cc -> {
				//FIXME: little bobby tables
				cc.addSql(
					"UPDATE AUTHOR SET NAME='" + authorUpdater.newName+"'");
			});
		}

		Stream<BookContext> addedBooks = authorUpdater.addedBooks;

		if (addedBooks != null) {
			command = command.andThen(cc -> {
				StringBuilder sb = new StringBuilder();

				AuthorQuerier authorQuerier = cc.get();

				String authorId = authorQuerier.id();

				addedBooks.forEach(bc -> {
					Long bookId = bc.andMap(BookQuerier::id).get();

					sb.append("INSERT INTO AUTHOR_BOOK (AUTHORID, BOOKID) VALUES (" + authorId + ", " + bookId + ");");
				});

				cc.addInsertSql(sb.toString());
			});
		}

		return command;
	}

	public Stream<AuthorContext> fromBook(BookContext bookContext) {
		Result<Long> idResult = bookContext.andMap(BookQuerier::id);

		if (idResult.getErrors().size() > 0) {
			return Stream.<AuthorContext>empty();
		}

		try {

			PreparedStatement preparedStatement = conn.prepareStatement(
				"SELECT * FROM AUTHOR A INNER JOIN AUTHOR_BOOK AB ON " +
					"A.id=AB.authorId WHERE AB.bookId = ?");

			preparedStatement.setLong(1, idResult.get());

			ResultSet resultSet = preparedStatement.executeQuery();

			return StreamSupport.stream(new Spliterator<AuthorContext>() {
				@Override
				public boolean tryAdvance(
					Consumer<? super AuthorContext> action) {

					try {
						if (!resultSet.next())  {
							return false;
						}
						action.accept(
							new OnlyAuthorContext(
									AuthorService.this,
									Long.toString(resultSet.getLong("id"))));
					}
					catch (SQLException e) {
						e.printStackTrace();

						return false;
					}

					return true;
				}

				@Override
				public Spliterator<AuthorContext> trySplit() {
					return null;
				}

				@Override
				public long estimateSize() {
					return -1;
				}

				@Override
				public int characteristics() {
					return IMMUTABLE;
				}
			}, false);
		}
		catch (SQLException e) {
			e.printStackTrace();

			throw new RuntimeException(e);
		}
	}
}
