class User
  include Mongoid::Document
  
  field :access_token, type: String
  field :gcm_token, type: String
  
  has_many :sns_accounts, class_name: SnsAccount.name, inverse_of: :user
  embeds_many :friend_lists, class_name: FriendList.name, inverse_of: :user
  embeds_many :requests, class_name: Request.name, inverse_of: :user
end