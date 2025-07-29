package researchstack.domain.usecase

import researchstack.domain.model.UserProfile
import researchstack.domain.model.priv.PrivDataType
import researchstack.domain.repository.DataSenderRepository
import javax.inject.Inject

class SendUserProfileUseCase @Inject constructor(
    private val dataSenderRepository: DataSenderRepository,
) {
    suspend operator fun invoke(userProfile: UserProfile) =
        dataSenderRepository.sendData(userProfile, PrivDataType.WEAR_USER_PROFILE)
}
