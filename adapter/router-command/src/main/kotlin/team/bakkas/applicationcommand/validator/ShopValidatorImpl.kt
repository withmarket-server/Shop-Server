package team.bakkas.applicationcommand.validator

import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import team.bakkas.clientcommand.shop.ShopCommand
import team.bakkas.clientcommand.shop.annotations.ShopCreatable
import team.bakkas.common.error.ErrorResponse
import team.bakkas.common.exceptions.RegionNotKoreaException
import team.bakkas.common.exceptions.RequestFieldException
import team.bakkas.common.exceptions.shop.ShopBranchInfoInvalidException
import team.bakkas.servicecommand.validator.ShopValidator

// Shop을 검증하는 로직을 정의하는 클래스
@Component
class ShopValidatorImpl : ShopValidator() {

    // reject Validate의 타겟을 정해주는 메소드
    override fun supports(clazz: Class<*>): Boolean {
        return ShopCommand.CreateRequest::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        target::class.java.annotations.map {
            // class에 따라 검증 로직을 분기한다
            when(it) {
                is ShopCreatable -> rejectEmptyByFieldList(
                    errors,
                    listOf("shopName", "openTime", "closeTime", "lotNumberAddress", "roadNameAddress", "latitude", "longitude", "shopCategory", "shopDetailCategory")
                )
            }
        }

        // Field에 error가 발생하여 errors에 content가 채워져서 보내진 경우 RequestFieldException을 일으킨다
        check(errors.allErrors.isEmpty()) {
            val errorList = errors.allErrors.map {
                ErrorResponse.FieldError.of(
                    it.objectName,
                    it.arguments.contentToString(),
                    it.defaultMessage!!
                )
            }
            throw RequestFieldException(errorList, "잘못된 요청입니다")
        }
    }

    // 해당 가게가 생성 가능한지 검증하는 메소드
    override fun validateCreatable(request: ShopCommand.CreateRequest) = with(request) {
        val errors = BeanPropertyBindingResult(this, ShopCommand.CreateRequest::class.java.name)

        // 우선 field에 대해서 검증한다
        validate(this, errors)

        check(validateIsInSouthKorea(request.latitude, request.longitude)) {
            throw RegionNotKoreaException("주어진 좌표가 한국(South Korea)내에 존재하지 않습니다.")
        }

        check(validateBranchInfo(request.isBranch, request.branchName)) {
            throw ShopBranchInfoInvalidException("본점/지점 정보가 잘못 주어졌습니다.")
        }
    }

    // 해당 가게가 한국에 존재하는 가게인지 검증하는 메소드
    private fun validateIsInSouthKorea(latitude: Double, longitude: Double): Boolean {
        val latitudeSatisfied = latitude > 125.06666667 && latitude < 131.87222222
        val longitudeSatisfied = longitude > 33.10000000 && longitude < 38.45000000

        return latitudeSatisfied && longitudeSatisfied
    }

    // 해당 가게의 지점 정보가 올바른지 검증하는 메소드
    private fun validateBranchInfo(isBranch: Boolean, branchName: String?): Boolean {
        // 1. 본점인데 branchName이 있는 경우
        val firstCondition = (isBranch && !branchName.isNullOrEmpty())
        // 2. 분점인데 branchName이 없는 경우
        val secondCondition = (!isBranch && branchName.isNullOrEmpty())

        return firstCondition || secondCondition
    }
}