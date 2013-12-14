class Friend
  include Mongoid::Document

  # data
  field :service, type: String
  
  # relations
  belongs_to :friend_user, class_name: User.name, inverse_of: :in_friends 
  belongs_to :user, class_name: User.name, inverse_of: :friends
  
end