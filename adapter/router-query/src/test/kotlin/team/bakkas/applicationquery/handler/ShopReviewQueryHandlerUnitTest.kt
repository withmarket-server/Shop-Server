package team.bakkas.applicationquery.handler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import team.bakkas.common.exceptions.RequestParamLostException
import team.bakkas.common.exceptions.shopReview.ShopReviewNotFoundException
import team.bakkas.domainquery.service.ifs.ShopReviewQueryService
import team.bakkas.dynamo.shopReview.ShopReview

@ExtendWith(MockKExtension::class)
internal class ShopReviewQueryHandlerUnitTest {
    private lateinit var shopReviewQueryHandler: ShopReviewQueryHandler

    private lateinit var shopReviewService: ShopReviewQueryService

    @BeforeEach
    fun setUp() {
        shopReviewService = mockk(relaxed = true)
        shopReviewQueryHandler = spyk(ShopReviewQueryHandler(shopReviewService))
    }

    @Test
    @DisplayName("[findReviewByIdAndTitle] 1. reviewId가 유실되어 RequestParamLostException을 일으키는 테스트")
    fun findReviewByIdAndTitleTest1(): Unit = runBlocking {
        // given
        val reviewId = ""
        val reviewTitle = "review title"
        val request = MockServerRequest.builder()
            .queryParam("id", reviewId)
            .queryParam("title", reviewTitle)
            .build()

        // then
        shouldThrow<RequestParamLostException> { shopReviewQueryHandler.findReviewById(request) }
    }

    @Test
    @DisplayName("[findReviewByIdAndTitle] 3. reviewId와 reviewTitle 모두 유실되어 RequestParamLostException을 일으키는 테스트")
    fun findReviewByIdAndTitleTest3(): Unit = runBlocking {
        // given
        val reviewId = ""
        val reviewTitle = ""
        val request = MockServerRequest.builder()
            .queryParam("id", reviewId)
            .queryParam("title", reviewTitle)
            .build()

        // then
        shouldThrow<RequestParamLostException> { shopReviewQueryHandler.findReviewById(request) }
    }

    @Test
    @DisplayName("[findReviewByIdAndTitle] 4. reviewId, reviewTitle에 매칭되는 review가 존재하지 않아 ShopReviewNotFoundException을 일으키는 테스트")
    fun findReviewByIdAndTitleTest4(): Unit = runBlocking {
        // given
        val reviewId = "fake id"
        val reviewTitle = "fake title"
        val request = MockServerRequest.builder()
            .queryParam("id", reviewId)
            .queryParam("title", reviewTitle)
            .build()

        coEvery { shopReviewService.findReviewById(reviewId) } returns null

        // then
        shouldThrow<ShopReviewNotFoundException> { shopReviewQueryHandler.findReviewById(request) }
    }

    @Test
    @DisplayName("[findReviewByIdAndTitle] 5. 성공 테스트")
    fun findReviewByIdAndTitleSuccess(): Unit = runBlocking {
        // given
        val reviewId = "fake id"
        val reviewTitle = "fake title"
        val request = MockServerRequest.builder()
            .queryParam("id", reviewId)
            .queryParam("title", reviewTitle)
            .build()

        coEvery { shopReviewService.findReviewById(reviewId) } returns
                getMockReview(reviewId, reviewTitle, "shop id", "shop name")

        // when
        val result = shopReviewQueryHandler.findReviewById(request)

        // then
        coVerify(exactly = 1) { shopReviewService.findReviewById(reviewId) }
        coVerify(exactly = 1) { shopReviewQueryHandler.findReviewById(request) }
        assertEquals(result.statusCode(), HttpStatus.OK)
    }

    @Test
    @DisplayName("[getReviewListByShopIdAndName] 1. shop-id가 빈 상태로 들어와서 RequestParamLostException을 일으키는 테스트")
    fun getReviewListByShopTest1(): Unit = runBlocking {
        // given
        val shopId = ""
        val request = MockServerRequest.builder()
            .queryParam("shop-id", shopId)
            .build()

        // then
        shouldThrow<RequestParamLostException> { shopReviewQueryHandler.getReviewListByShopId(request) }
    }

    @Test
    @DisplayName("[getReviewListByShopIdAndName] 4. shopId에 대응하는 review가 존재하지 않는 경우")
    fun getReviewListByShopTest4(): Unit = runBlocking {
        // given
        val shopId = "shop-id"
        val request = MockServerRequest.builder()
            .queryParam("shop-id", shopId)
            .build()

        // 비어있는 리스트를 반환한다
        coEvery { shopReviewService.getReviewsByShopId(shopId) } returns listOf()

        // then
        shouldThrow<ShopReviewNotFoundException> { shopReviewQueryHandler.getReviewListByShopId(request) }
    }

    @Test
    @DisplayName("[getReviewListByShopIdAndName] 성공 테스트")
    fun getReviewListByShopSuccess(): Unit = runBlocking {
        // given
        val shopId = "shop-id"
        val shopName = "shop-name"
        val request = MockServerRequest.builder()
            .queryParam("shop-id", shopId)
            .build()

        // 리뷰가 존재하는 경우
        coEvery { shopReviewService.getReviewsByShopId(shopId) } returns
                listOf(
                    getMockReview("1", "review1", shopId, shopName),
                    getMockReview("2", "review2", shopId, shopName)
                )

        // when
        val response = shopReviewQueryHandler.getReviewListByShopId(request)

        // then
        coVerify(exactly = 1) { shopReviewService.getReviewsByShopId(shopId) }
        coVerify(exactly = 1) { shopReviewQueryHandler.getReviewListByShopId(request) }
        assertEquals(response.statusCode(), HttpStatus.OK)
    }

    // 가짜 review를 반환하는 메소드
    private fun getMockReview(reviewId: String, reviewTitle: String, shopId: String, shopName: String) =
        ShopReview(
            reviewId = reviewId,
            reviewTitle = reviewTitle,
            shopId = shopId,
            reviewContent = "저는 아주 불만족했어요! ^^",
            reviewScore = 1.0,
            reviewPhotoList = listOf()
        )
}