package team.bakkas.domaindynamo.validator

import org.springframework.core.CoroutinesUtils
import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import team.bakkas.common.Results
import team.bakkas.common.exceptions.RequestFieldException
import team.bakkas.common.exceptions.shop.ShopNotFoundException
import team.bakkas.common.urls.ServerUrlsInterface
import team.bakkas.domaindynamo.entity.ShopReview

/** Shop Review에 대한 검증을 수행하는 Validator class
 * @param urlComponent 활성화된 환경에 따라 url을 변동적으로 관리해주는 bean class. local/server 환경을 분리해서 관리한다
 */
@Component
class ShopReviewValidator(
    private val urlComponent: ServerUrlsInterface
): Validator {

    private val shopWebClient = WebClient.create(urlComponent.SHOP_QUERY_SERVER_URL)

    // 해당 리뷰가 생성 가능한지 검증하는 메소드
    suspend fun validateCreatable(shopReview: ShopReview) = with(shopReview) {
        validateFirst(this) // 우선 필드를 모두 검증한다

        // WebClient를 이용해서 해당 shop이 존재하는지 여부만 뽑아온다
        val shopResultMono: Mono<Boolean> = isExistsShop(shopId, shopName)
        val shopResultDeferred = CoroutinesUtils.monoToDeferred(shopResultMono)

        // shop이 존재하지 않는 경우 예외를 발생시킨다
        check(shopResultDeferred.await()) {
            throw ShopNotFoundException("shop review에 대응하는 shop이 존재하지 않습니다.")
        }
    }

    override fun supports(clazz: Class<*>): Boolean {
        return ShopReview::class.java.isAssignableFrom(clazz)
    }

    // reviewId, reviewTitle, shopId, shopName, reviewContent : 비어있는지 검증
    // reviewContent는 200자 이상으로는 못 쓰도록 검증한다
    override fun validate(target: Any, errors: Errors) {
        // reviewId, reviewTitle, shopId, shopName, reviewContent : 비어있는지 검증
        listOf("reviewId, reviewTitle, shopId, shopName, reviewContent").forEach { fieldName ->
            ValidationUtils.rejectIfEmpty(errors, fieldName, "field.required", "${fieldName}이 제공되지 않았습니다.")
        }

        val review = target as ShopReview

        // reviewContent의 길이를 200으로 제한한다
        check(review.reviewContent.length <= 200) {
            errors.rejectValue("reviewContent", "field.max.length", "review의 내용은 200을 넘어서는 안됩니다.")
        }
    }

    // 기본적으로 검증해야하는 메소드
    private fun validateFirst(shopReview: ShopReview) = with(shopReview) {
        val errors = BeanPropertyBindingResult(this, ShopReview::class.java.name)
        validate(this, errors)

        // 기본 조건들을 만족하지 못하면 exception을 터뜨린다
        check(errors == null || errors.allErrors.isEmpty()) {
            throw RequestFieldException(errors.allErrors.toString())
        }
    }

    private fun isExistsShop(shopId: String, shopName: String): Mono<Boolean> {
        return shopWebClient.get()
            .uri(
                UriComponentsBuilder
                    .fromUriString("/v2/shop/simple")
                    .queryParam("id", shopId)
                    .queryParam("name", shopName)
                    .toUriString()
            ).retrieve()
            .bodyToMono(Results.SingleResult::class.java)
            .map { result -> result.success }
    }
}