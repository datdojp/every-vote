class User
  include Mongoid::Document
  
  # data
  field :gcm_token, type: String
  field :default_service, type: String
  
  # relations
  embeds_many :profiles, class_name: UserProfile.name, inverse_of: :user
  
  has_many :requests, class_name: Request.name, inverse_of: :sender
  has_and_belongs_to_many :in_requests, class_name: Request.name, inverse_of: :target_users
  
  has_many :groups, class_name: Group.name, inverse_of: :user
  has_and_belongs_to_many :in_groups, class_name: Group.name, inverse_of: :group_users
  
  has_many :friends, class_name: Friend.name, inverse_of: :user
  has_many :in_friends, class_name: Friend.name, inverse_of: :friend_user
end