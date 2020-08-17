package io.paytouch.core.entities

import akka.http.scaladsl.model.Uri
import io.paytouch.core.utils.PaytouchSpec
import org.specs2.specification.{ Scope => SpecScope }

class PaginationSpec extends PaytouchSpec {

  abstract class PagerSpecContext extends SpecScope with Fixtures

  "Pagination" should {
    "return a list of navigation links" in new PagerSpecContext {
      val complexUri = Uri("https://example.com:88/v1/p?per_page=10&page=3&other=preserveMe")
      PaginationLinks(pagination, complexUri, totalCount) ==== PaginationLinks(
        prev = Some("https://example.com:88/v1/p?per_page=10&page=2&other=preserveMe"),
        next = Some("https://example.com:88/v1/p?per_page=10&page=4&other=preserveMe"),
        first = "https://example.com:88/v1/p?per_page=10&page=1&other=preserveMe",
        last = "https://example.com:88/v1/p?per_page=10&page=6&other=preserveMe",
        perPage = perPage,
        totalCount = totalCount,
      )
    }

    "return a list of navigation links with no previous link for first page" in new PagerSpecContext {
      val firstPageUri = Uri("https://example.com:88/v1/p?per_page=10&other=preserveMe")
      PaginationLinks(pagination, firstPageUri, totalCount) ==== PaginationLinks(
        prev = None,
        next = Some("https://example.com:88/v1/p?per_page=10&other=preserveMe&page=2"),
        first = "https://example.com:88/v1/p?per_page=10&other=preserveMe&page=1",
        last = "https://example.com:88/v1/p?per_page=10&other=preserveMe&page=6",
        perPage = perPage,
        totalCount = totalCount,
      )
    }

    "return a list of navigation links with no next link for last page" in new PagerSpecContext {
      val lastPageUri = Uri("https://example.com:88/v1/p?per_page=10&page=6&other=preserveMe")
      PaginationLinks(pagination, lastPageUri, totalCount) ==== PaginationLinks(
        prev = Some("https://example.com:88/v1/p?per_page=10&page=5&other=preserveMe"),
        next = None,
        first = "https://example.com:88/v1/p?per_page=10&page=1&other=preserveMe",
        last = "https://example.com:88/v1/p?per_page=10&page=6&other=preserveMe",
        perPage = perPage,
        totalCount = totalCount,
      )
    }

    "return a list of navigation links with no next/prev links for a single page" in new PagerSpecContext {
      val pageUri = Uri("https://example.com:88/v1/p?per_page=10&page=1&other=preserveMe")
      val onePageTotalCount = 8
      val onePagePagination = Pagination(page = 1, perPage = 10)
      PaginationLinks(onePagePagination, pageUri, onePageTotalCount) ==== PaginationLinks(
        prev = None,
        next = None,
        first = "https://example.com:88/v1/p?per_page=10&page=1&other=preserveMe",
        last = "https://example.com:88/v1/p?per_page=10&page=1&other=preserveMe",
        perPage = 10,
        totalCount = onePageTotalCount,
      )
    }

    "page_limit param" should {
      "limit the page count" in new PagerSpecContext {
        val complexUri = Uri("https://example.com:88/v1/p?per_page=10&page=3&page_limit=3&other=preserveMe")
        PaginationLinks(pagination, complexUri, totalCount) ==== PaginationLinks(
          prev = Some("https://example.com:88/v1/p?per_page=10&page=2&page_limit=3&other=preserveMe"),
          next = None,
          first = "https://example.com:88/v1/p?per_page=10&page=1&page_limit=3&other=preserveMe",
          last = "https://example.com:88/v1/p?per_page=10&page=3&page_limit=3&other=preserveMe",
          perPage = perPage,
          totalCount = 30,
        )
      }

      "be ignored if greater than the number of pages" in new PagerSpecContext {
        val complexUri = Uri("https://example.com:88/v1/p?per_page=10&page=3&page_limit=1000&other=preserveMe")
        PaginationLinks(pagination, complexUri, totalCount) ==== PaginationLinks(
          prev = Some("https://example.com:88/v1/p?per_page=10&page=2&page_limit=1000&other=preserveMe"),
          next = Some("https://example.com:88/v1/p?per_page=10&page=4&page_limit=1000&other=preserveMe"),
          first = "https://example.com:88/v1/p?per_page=10&page=1&page_limit=1000&other=preserveMe",
          last = "https://example.com:88/v1/p?per_page=10&page=6&page_limit=1000&other=preserveMe",
          perPage = perPage,
          totalCount = totalCount,
        )
      }
    }
  }

  trait Fixtures {
    val perPage = 10
    val currentPage = 3
    val pagination = Pagination(currentPage, perPage)
    val totalCount = 55
  }
}
