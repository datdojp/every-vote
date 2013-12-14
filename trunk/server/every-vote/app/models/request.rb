class Request
  include Mongoid::Document

  # data
  field :message, type: String
  field :n_votes, type: String
  field :starts_at, type: Integer # in seconds since 1970
  field :expires_in, type: Integer # in seconds

  # relations
  belongs_to :sender, class_name: User.name, inverse_of: :requests
  has_and_belongs_to_many :target_users, class_name: User.name, inverse_of: :in_requests
end