package team.bakkas.repository.ifs.redis

import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Mono
import team.bakkas.dynamo.shop.Shop

interface ShopRedisRepository {

    // shop을 캐싱하는 메소드
    fun cacheShop(shop: Shop): Mono<Shop>

    // redis에 저장된 shop을 가져오는 메소드
    fun findShopByKey(shopKey: String): Mono<Shop>

    // redis 상에 캐싱된 모든 shop을 가져오는 메소드
    fun getAllShops(): Flow<Shop>
}