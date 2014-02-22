class FriendList
  include Mongoid::Document

  field :name, type: String
  
  embeds_many :friends, class_name: Friend.name, inverse_of: :friend_list
  belongs_to :user, class_name: User.name, inverse_of: :friend_lists
end