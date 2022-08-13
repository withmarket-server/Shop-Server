package team.bakkas.domainqueryservice.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.springframework.core.CoroutinesUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.bakkas.common.exceptions.shopReview.ShopReviewNotFoundException
import team.bakkas.domaindynamo.entity.ShopReview
import team.bakkas.domainqueryservice.repository.ifs.ShopReviewReader
import team.bakkas.domainqueryservice.service.ifs.ShopReviewQueryService

@Service
class ShopReviewQueryServiceImpl(
    private val shopReviewReader: ShopReviewReader
) : ShopReviewQueryService {

    /** reviewId와 reviewTitle을 기반으로 ShopReview를 가져오는 메소드
     * @param reviewId review id
     * @param reviewTitle review title
     * @throws ShopReviewNotFoundException
     * @return ShopReview
     */
    @Transactional(readOnly = true)
    override suspend fun findReviewByIdAndTitle(reviewId: String, reviewTitle: String): ShopReview =
        withContext(Dispatchers.IO) {
            val reviewMono = shopReviewReader.findShopReviewByIdAndTitleWithCaching(reviewId, reviewTitle)
            val reviewDeferred = CoroutinesUtils.monoToDeferred(reviewMono)

            return@withContext reviewDeferred.await() ?: throw ShopReviewNotFoundException("review is not found!!")
        }

    @Transactional(readOnly = true)
    override suspend fun getReviewListByShop(shopId: String, shopName: String): List<ShopReview> =
        withContext(Dispatchers.IO) {
            val reviewFlow = shopReviewReader.getShopReviewListFlowByShopIdAndNameWithCaching(shopId, shopName)

            // flow에 item이 하나도 전달이 안 되는 경우의 예외 처리
            try {
                val firstItem = reviewFlow.firstOrNull()
                checkNotNull(firstItem)
            } catch (_: Exception) {
                throw ShopReviewNotFoundException("Shop review is not found!!")
            }

            val reviewList = reviewFlow.toList()

            // review가 하나도 안 모였다면 바로 에러 처리
            check(reviewList.size != 0) {
                throw ShopReviewNotFoundException("Shop review is not found!!")
            }

            reviewList
        }
}